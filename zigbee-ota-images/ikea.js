/**
 * Download Zigbee OTA images for IKEA devices
 * 
 * @see http://fw.ota.homesmart.ikea.net/feed/version_info.json
 * @see https://github.com/Koenkk/zigbee-OTA/blob/master/lib/ota.js
 */
const path = require('path');
const fs = require('fs');
const assert = require('assert');

const upgradeFileIdentifier = Buffer.from([0x1E, 0xF1, 0xEE, 0x0B]);

const downloadFile = async (urlPath, filename) => {
    console.log(`Downloading ${urlPath} -> ${filename}`)

    const lib = require('http');
    const file = fs.createWriteStream(filename);

    return new Promise((resolve, reject) => {
        const options = {
            hostname: 'fw.ota.homesmart.ikea.net',
            path: urlPath,
            headers: {
                'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8',
                'Accept-Encoding': 'gzip, deflate',
                'Accept-Language': 'en-US,en;q=0.5',
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/118.0'
            }
        }
        const request = lib.get(options, function(response) {
            if (response.statusCode >= 200 && response.statusCode < 300) {
                response.pipe(file);
                file.on('finish', function() {
                  file.close(function() {
                      resolve();
                  });
                });
            } else if (response.headers.location) {
                resolve(downloadFile(response.headers.location, filename));
            } else {
                console.log(response)
                reject(new Error(response.statusCode + ' ' + response.statusMessage));
            }
        });
    });
}

function parseSubElement(buffer, position) {
    const tagID = buffer.readUInt16LE(position);
    const length = buffer.readUInt32LE(position + 2);
    const data = buffer.slice(position + 6, position + 6 + length);
    return {tagID, length, data};
}

function parseImage(rawBuffer) {
    const start = rawBuffer.indexOf(upgradeFileIdentifier);
    const buffer = rawBuffer.slice(start);

    const header = {
        otaUpgradeFileIdentifier: buffer.subarray(0, 4),
        otaHeaderVersion: buffer.readUInt16LE(4),
        otaHeaderLength: buffer.readUInt16LE(6),
        otaHeaderFieldControl: buffer.readUInt16LE(8),
        manufacturerCode: buffer.readUInt16LE(10),
        imageType: buffer.readUInt16LE(12),
        fileVersion: buffer.readUInt32LE(14),
        zigbeeStackVersion: buffer.readUInt16LE(18),
        otaHeaderString: buffer.toString('utf8', 20, 52),
        totalImageSize: buffer.readUInt32LE(52),
    };
    let headerPos = 56;
    if (header.otaHeaderFieldControl & 1) {
        header.securityCredentialVersion = buffer.readUInt8(headerPos);
        headerPos += 1;
    }
    if (header.otaHeaderFieldControl & 2) {
        header.upgradeFileDestination = buffer.subarray(headerPos, headerPos + 8);
        headerPos += 8;
    }
    if (header.otaHeaderFieldControl & 4) {
        header.minimumHardwareVersion = buffer.readUInt16LE(headerPos);
        headerPos += 2;
        header.maximumHardwareVersion = buffer.readUInt16LE(headerPos);
        headerPos += 2;
    }

    const raw = buffer.slice(0, header.totalImageSize);

    assert(Buffer.compare(header.otaUpgradeFileIdentifier, upgradeFileIdentifier) === 0, 'Not an OTA file');

    let position = header.otaHeaderLength;
    const elements = [];
    while (position < header.totalImageSize) {
        const element = parseSubElement(buffer, position);
        elements.push(element);
        position += element.data.length + 6;
    }

    assert(position === header.totalImageSize, 'Size mismatch');
    return {header, elements, raw};
}

const main = async () => {
    if (!fs.existsSync('temp')) fs.mkdirSync('temp');
    if (!fs.existsSync('images')) fs.mkdirSync('images');

    const jsonFile = path.resolve('temp/version_info.json');
    await downloadFile('/feed/version_info.json', jsonFile);

    const jsonData = JSON.parse(fs.readFileSync(jsonFile));
    for (const entry of jsonData) {
        const urlPath = entry.fw_binary_url.replaceAll('http://fw.ota.homesmart.ikea.net', '');
        const filename = `temp/${urlPath.split('/').pop()}`;

        // Download signed OTA file
        if (!filename.endsWith('.ota.ota.signed')) continue;
        await downloadFile(urlPath, filename);

        // Read file data
        const buffer = fs.readFileSync(filename);
        const parsed = parseImage(buffer);
        console.log(parsed.header);

        // Build output file name
        const fwVersion = filename.replaceAll('-prod', '').replaceAll('.ota.ota.signed', '').split('-').pop()
        const manufacturerCode = parsed.header.manufacturerCode.toString(16).toUpperCase();
        const imageType = parsed.header.imageType.toString(16).toUpperCase();
        const fileVersion = parsed.header.fileVersion.toString(16).toUpperCase().padStart(8, 0);
        const deviceName = parsed.header.otaHeaderString.replaceAll('EBL ', '').replaceAll('GBL ', '').replaceAll('GBL_', '').replaceAll("\x00", '')
        const outputName = `images/${manufacturerCode}-${imageType}-${fileVersion}-${deviceName}-${fwVersion}.zigbee`
        console.log(`output = ${outputName}`)

        // Write to output file
        fs.writeFileSync(outputName, parsed.raw);
        delete(parsed.header.otaUpgradeFileIdentifier);
        fs.writeFileSync(`${outputName}.txt`, `Source: ${entry.fw_binary_url}\nImage Details: ${JSON.stringify(parsed.header, null, "    ")}`);
    };

    // Cleanup
    fs.rmSync('temp', { recursive: true, force: true });
}

main();

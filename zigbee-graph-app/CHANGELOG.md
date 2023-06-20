# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2023-06-20
### Changed
- Migrate to the new endpoint `getChildAndRouteInfoJson` to remove text parsing and HTML scraping
- Other small UI improvements

### Added
- Use [Solarized](https://ethanschoonover.com/solarized/) theme colors
- Add support for Dark theme
- Add config option to hide link particles and how directional arrows instead (helps when sharing a graph image)
- Add FAQ section to try to add some meaning to the graph (content is taken mostly from the Hubitat community, thanks!)

## [1.4.0] - 2023-05-25
### Added
- Add the "Config" tab with basic settings

## [1.3.1] - 2023-05-24
### Fixed
- Devices table keeps accumulating records instead of clearing its contents

## [1.3.0] - 2023-05-23
### Added
- Add a new tab containing a list with all zigbee devices

## [1.2.0] - 2023-05-23
### Added
- Click a node to go to the device edit page
- Add `embed=true` URL parameter to hide controls

### Changed
- Make application interface friendlier

## [1.1.0] - 2023-05-23
### Added
- Integrate zigbee logs into the graph

## [1.0.0] - 2023-05-22
### Added
- Initial release

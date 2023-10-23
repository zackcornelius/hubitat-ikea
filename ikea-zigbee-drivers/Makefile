
.PHONY: all clean

all: E1603.groovy  E1743.groovy  E1745.groovy  E1766.groovy  E1810.groovy  E1812.groovy  E1836.groovy  E2002.groovy  E2123.groovy  E2201.groovy  E1766.groovy ICPSHC24.groovy

%.groovy: src/devices/%/config.yaml src/common.yaml src/blueprint.groovy
	tools/yaml-merge src/common.yaml $< | tools/mustache src/blueprint.groovy > $@

clean:
	rm -rf *.groovy

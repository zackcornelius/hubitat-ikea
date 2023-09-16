# IKEA Zigbee drivers - Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.3.0] - 2023-09-16
### Added
 - Add driver for IKEA Tradfri Motion Sensor (E1745)

## [2.2.0] - 2023-09-15
### Added
 - E1603: Configure what happens after a power outage (Turn On, Turn Off, Restore previous state)

## [2.1.0] - 2023-09-15
### Added
 - E2002: Add support for firmware `2.4.5`

## [2.0.1] - 2023-09-14
### Fixed
 - Fix drivers version in the UI

## [2.0.0] - 2023-09-14
### Changed
 - Major rewrite using {{ Mustache }} templates

### Added
- Add driver for Tradfri Control Outlet (E1603)
- Add driver for Tradfri On/Off Switch (E1743)
- Add driver for Tradfri Shortcut Button (E1812)
- Add driver for Styrbar Remote Control N2 (E2002)
- Add driver for Rodret Dimmer (E2201)

## [1.3.0] - 2023-09-06
### Added
- Add `healthStatus` attribute

## [1.2.0] - 2023-09-05
### Changed
- Cleanup drivers code

## [1.1.0] - 2023-09-03
### Added
- E1810: Add driver for Tradfri Remote Control (E1810)
- E2123: Add "ReleasableButton" capability for the dots buttons

## [1.0.1] - 2023-09-02
### Fixed
- Fix battery reporting on firmware 1.0.012
- Fix logging automatically reverting back to "Info"

## [1.0.0] - 2023-09-01
### Added
- Initial driver for IKEA Symfonisk Sound Remote Gen2 (E2123)

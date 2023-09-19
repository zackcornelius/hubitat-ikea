{{!--------------------------------------------------------------------------}}
{{# definition }}
capability "PowerSource"
{{/ definition }}
{{!--------------------------------------------------------------------------}}
{{# configuration }}

// capability.PowerSource
{{# battery }}
sendEvent name:"powerSource", value:"battery", descriptionText:"Power source is battery"
{{/ battery }}
{{# mains }}
sendEvent name:"powerSource", value:"mains", descriptionText:"Power source is mains"
{{/ mains }}
{{/ configuration }}
{{!--------------------------------------------------------------------------}}
{{# configure }}

// Configuration for capability.PowerSource
sendEvent name:"powerSource", value:"{{ type }}", descriptionText:"Power source is {{ type }}"
{{/ configure }}
{{!--------------------------------------------------------------------------}}

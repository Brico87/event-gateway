package app.rbac

import rego.v1

default allow := false

allow if {
	partner_can_read
	partner_is_allowed
}

partner_can_read if {
	# `permission` is assigned as element of the partner_permissions for this partner
	some permission in data.partners[input.partner].permissions

	# `action` must be "read" for the matched resource
	permission.resource == input.resource
	permission.action == "read"
}

partner_is_allowed if {
	# input.partner == "toto"

	partner_location := data.partners[input.partner].location
	partner_location.region == input.user.region
	partner_location.department == input.user.department
}

package app.rbac

import rego.v1

default allow := false

allow if {
	user_can_read
	user_is_allowed
}

user_can_read if {
	# `permission` is assigned as element of the user_permissions for this user
	some permission in data.users[input.user].permissions

	# `action` must be "read" for the matched resource
	permission.resource == input.resource
	permission.action == "read"
}

user_is_allowed if {
	# input.user == "toto"

	user_location := data.users[input.user].location
	user_location.region == input.region
	user_location.department == input.department
}

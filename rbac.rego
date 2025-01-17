package app.rbac

import rego.v1

default allow := false

allow if {
    input.user == "toto"
}
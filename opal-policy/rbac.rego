package app.rbac

default allow = false

allow if {
    input.user == "toto"
}
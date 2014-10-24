#!/bin/bash
gen_java_types -s src -S ./NJSMock.spec
compile_typespec --js NJSMock ./NJSMock.spec lib

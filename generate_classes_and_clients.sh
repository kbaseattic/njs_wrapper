#!/bin/bash
gen_java_types -s src -S ./NJSMock.spec
compile_typespec --js NarrativeJobServiceClient ./NJSMock.spec lib

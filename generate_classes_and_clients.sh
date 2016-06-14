#!/bin/bash
gen_java_types -s src -S ./NJSWrapper.spec
compile_typespec --js NarrativeJobServiceClient ./NJSWrapper.spec lib

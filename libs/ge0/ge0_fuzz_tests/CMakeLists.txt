project(ge0_fuzz_tests)

set(SRC
  ge0_fuzz_tests.cpp
)

omim_add_executable(${PROJECT_NAME} ${SRC})

target_link_libraries(${PROJECT_NAME} ge0)

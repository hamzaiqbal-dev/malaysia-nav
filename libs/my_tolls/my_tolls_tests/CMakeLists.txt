project(my_tolls_tests)

set(SRC
  toll_booths_tests.cpp
)

omim_add_test(${PROJECT_NAME} ${SRC})

target_link_libraries(${PROJECT_NAME} PRIVATE my_tolls)

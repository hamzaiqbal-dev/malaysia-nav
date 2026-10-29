project(my_tolls)

set(SRC
  toll_booths.cpp
  toll_booths.hpp
)

omim_add_library(${PROJECT_NAME} ${SRC})
target_link_libraries(${PROJECT_NAME}
  PUBLIC
    indexer
    geometry

  PRIVATE
    base
)

omim_add_test_subdirectory(my_tolls_tests)

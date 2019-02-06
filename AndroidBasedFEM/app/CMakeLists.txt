# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             native-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/native-lib.cpp

        src/main/cpp/FEMPackage/Geokernel/AbsGeometry.h
        src/main/cpp/FEMPackage/Geokernel/Rectangle.cpp
        src/main/cpp/FEMPackage/Geokernel/Rectangle.h
        src/main/cpp/FEMPackage/Geokernel/Vertex.cpp
        src/main/cpp/FEMPackage/Geokernel/Vertex.h
        src/main/cpp/FEMPackage/Geokernel/Edge.cpp
        src/main/cpp/FEMPackage/Geokernel/Edge.h
        src/main/cpp/FEMPackage/Geokernel/ProblemType.cpp
        src/main/cpp/FEMPackage/Geokernel/ProblemType.h
        src/main/cpp/FEMPackage/Geokernel/PrimaryTypes.h
        src/main/cpp/FEMPackage/Geokernel/Triangle.cpp
        src/main/cpp/FEMPackage/Geokernel/Triangle.h
        src/main/cpp/FEMPackage/Geokernel/AbsGeometryElement.h
        src/main/cpp/FEMPackage/Geokernel/ConvexHull.cpp
        src/main/cpp/FEMPackage/Geokernel/ConvexHull.h
        src/main/cpp/FEMPackage/Geokernel/GeometryTypes.h

        src/main/cpp/FEMPackage/Femkernel_Test.cpp
        src/main/cpp/FEMPackage/Femkernel/AbsMeshFactory.h
        src/main/cpp/FEMPackage/Femkernel/AbsMesher.h
        src/main/cpp/FEMPackage/Femkernel/AbsMeshFactory.cpp
        src/main/cpp/FEMPackage/Femkernel/AbsMesher.cpp
        src/main/cpp/FEMPackage/Femkernel/Mesher2D.h
        src/main/cpp/FEMPackage/Femkernel/Mesher2D.cpp
        src/main/cpp/FEMPackage/Femkernel/MeshFactory2D.h
        src/main/cpp/FEMPackage/Femkernel/MeshFactory2D.cpp
        src/main/cpp/FEMPackage/Femkernel/LinearTriangle.cpp
        src/main/cpp/FEMPackage/Femkernel/LinearTriangle.h
        src/main/cpp/FEMPackage/Femkernel/Assembler.h
        src/main/cpp/FEMPackage/Femkernel/Assembler.cpp
        src/main/cpp/FEMPackage/Femkernel/TriangularEFT.cpp
        src/main/cpp/FEMPackage/Femkernel/TriangularEFT.h
        src/main/cpp/FEMPackage/Femkernel/Types.h

        src/main/cpp/FEMPackage/Utilities/Bithacks.h
        src/main/cpp/FEMPackage/Utilities/Constants.h
        src/main/cpp/FEMPackage/Utilities/Json.hpp

        src/main/cpp/FEMPackage/Mathkernel/modalAnalysis.h

        src/main/cpp/FEMPackage/Postprocessing/PostProcess.h
        src/main/cpp/FEMPackage/Postprocessing/PostProcess.cpp
        src/main/cpp/FEMPackage/Postprocessing/Types.h

             )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       native-lib

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )

    # Sets the minimum version of CMake required to build your native library.
    # This ensures that a certain set of CMake features is available to
    # your build.

    cmake_minimum_required(VERSION 3.3.2)

    # Specifies a library name, specifies whether the library is STATIC or
    # SHARED, and provides relative paths to the source code. You can
    # define multiple libraries by adding multiple add_library() commands,
    # and CMake builds them for you. When you build your app, Gradle
    # automatically packages shared libraries with your APK.

	add_definitions(-DHAVE_CONFIG_H)

    add_library( # Specifies the name of the library.
                 native-library
                 # Sets the library as a shared library.
				 OBJECT
                 # Provides a relative path to your source file(s).
				 src/secp256k1.c
				 )

    add_library( # Specifies the name of the library.
                 native-secp256k1
                 # Sets the library as a shared library.
                 SHARED
                 # Provides a relative path to your source file(s).
                 io_taucoin_android_Secp256k1Context.c
				 io_taucoin_android_NativSecp256k1.c
				 # Linck native-library
				 $<TARGET_OBJECTS:native-library> )

	# Specifies a path to native header files.
    include_directories(src/include/)

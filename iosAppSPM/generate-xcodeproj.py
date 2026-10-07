#!/usr/bin/env python3
"""Generates iosAppSPM/RecipesExplorer.xcodeproj/project.pbxproj.

The app has no Xcode GUI session available while it is authored, so the project
file is generated from this script. Re-run it after adding or removing source
files:

    python3 iosAppSPM/generate-xcodeproj.py
"""

from __future__ import annotations

import os
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent
PROJECT_NAME = "RecipesExplorer"
APP_DIR = ROOT / "RecipesExplorer"
XCODEPROJ = ROOT / f"{PROJECT_NAME}.xcodeproj"
BUNDLE_ID = "com.example.recipesexplorer"
DEPLOYMENT_TARGET = "17.0"
SWIFT_VERSION = "5.0"

# Source files, grouped for the navigation pane.
SOURCE_GROUPS: dict[str, list[str]] = {
    "App": ["RecipesExplorerApp.swift"],
    "Store": ["Store/RecipesStore.swift", "Store/FavoritesStore.swift"],
    "Models": ["Models/RecipeItem.swift"],
    "Views": ["Views/RecipeListView.swift", "Views/RecipeDetailView.swift"],
}


def oid(seed: str) -> str:
    """Deterministic 24 character object id."""
    import hashlib

    return hashlib.sha1(seed.encode()).hexdigest()[:24].upper()


def file_type(name: str) -> str:
    if name.endswith(".swift"):
        return "sourcecode.swift"
    if name.endswith(".plist"):
        return "text.plist.xml"
    if name.endswith(".xcassets"):
        return "folder.assetcatalog"
    if name.endswith(".xcconfig"):
        return "text.xcconfig"
    return "text"


def collect_sources() -> list[str]:
    return [path for paths in SOURCE_GROUPS.values() for path in paths]


def main() -> None:
    sources = collect_sources()

    app_ref = oid("file:app-product")
    assets_ref = oid("file:assets")
    config_ref = oid("file:xcconfig")
    info_ref = oid("file:info")
    package_ref = oid("file:package")

    source_refs = {path: oid(f"file:{path}") for path in sources}
    source_build = {path: oid(f"build:{path}") for path in sources}
    group_ids = {name: oid(f"group:{name}") for name in SOURCE_GROUPS}
    main_group = oid("group:main")
    app_group = oid("group:app")
    products_group = oid("group:products")
    frameworks_group = oid("group:frameworks")

    target = oid("target:app")
    ui_test_target = oid("target:uitests")
    ui_test_product = oid("file:uitests-product")
    ui_test_group = oid("group:uitests")
    ui_test_file = oid("file:uitests-swift")
    ui_test_info = oid("file:uitests-info")
    ui_test_build = oid("build:uitests-swift")
    ui_test_sources = oid("phase:uitests-sources")
    ui_test_frameworks = oid("phase:uitests-frameworks")
    ui_test_resources = oid("phase:uitests-resources")
    ui_test_dependency = oid("dependency:uitests")
    ui_test_proxy = oid("proxy:uitests")
    config_list_uitest = oid("configlist:uitests")
    config_debug_uitest = oid("config:uitests:debug")
    config_release_uitest = oid("config:uitests:release")
    project = oid("project:root")
    product_dep = oid("product-dependency")
    package_dep = oid("package-dependency")
    sources_phase = oid("phase:sources")
    frameworks_phase = oid("phase:frameworks")
    resources_phase = oid("phase:resources")
    config_list_project = oid("configlist:project")
    config_list_target = oid("configlist:target")
    config_debug_project = oid("config:project:debug")
    config_release_project = oid("config:project:release")
    config_debug_target = oid("config:target:debug")
    config_release_target = oid("config:target:release")

    def build_settings_target(debug: bool) -> str:
        return f"""
				ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
				ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME = AccentColor;
				CODE_SIGNING_ALLOWED = NO;
				CODE_SIGNING_REQUIRED = NO;
				CODE_SIGN_IDENTITY = "";
				CODE_SIGN_STYLE = Automatic;
				CURRENT_PROJECT_VERSION = 1;
				ENABLE_PREVIEWS = YES;
				GENERATE_INFOPLIST_FILE = NO;
				INFOPLIST_FILE = "RecipesExplorer/Info.plist";
				IPHONEOS_DEPLOYMENT_TARGET = {DEPLOYMENT_TARGET};
				LD_RUNPATH_SEARCH_PATHS = (
					"$(inherited)",
					"@executable_path/Frameworks",
				);
				MARKETING_VERSION = 1.0;
				PRODUCT_BUNDLE_IDENTIFIER = {BUNDLE_ID};
				PRODUCT_NAME = "$(TARGET_NAME)";
				SWIFT_EMIT_LOC_STRINGS = YES;
				SWIFT_VERSION = {SWIFT_VERSION};
				TARGETED_DEVICE_FAMILY = "1,2";
		"""

    def build_settings_project(debug: bool) -> str:
        return f"""
				ALWAYS_SEARCH_USER_PATHS = NO;
				CLANG_ENABLE_MODULES = YES;
				CLANG_ENABLE_OBJC_ARC = YES;
				COPY_PHASE_STRIP = NO;
				DEBUG_INFORMATION_FORMAT = {"dwarf" if debug else "dwarf-with-dsym"};
				ENABLE_STRICT_OBJC_MSGSEND = YES;
				ENABLE_TESTABILITY = {"YES" if debug else "NO"};
				GCC_C_LANGUAGE_STANDARD = gnu17;
				GCC_DYNAMIC_NO_PIC = NO;
				GCC_NO_COMMON_BLOCKS = YES;
				GCC_OPTIMIZATION_LEVEL = {"0" if debug else "s"};
				GCC_PREPROCESSOR_DEFINITIONS = (
					"$(inherited)",
					"DEBUG=1",
				);
				IPHONEOS_DEPLOYMENT_TARGET = {DEPLOYMENT_TARGET};
				MTL_ENABLE_DEBUG_INFO = INCLUDE_SOURCE;
				ONLY_ACTIVE_ARCH = YES;
				SDKROOT = iphoneos;
				SWIFT_ACTIVE_COMPILATION_CONDITIONS = "DEBUG $(inherited)";
				SWIFT_OPTIMIZATION_LEVEL = "-Onone";
				SWIFT_VERSION = {SWIFT_VERSION};
		"""

    lines: list[str] = []
    add = lines.append

    add("// !$*UTF8*$!")
    add("{")
    add("\tarchiveVersion = 1;")
    add("\tclasses = {")
    add("\t};")
    add("\tobjectVersion = 60;")
    add("\tobjects = {")

    # PBXBuildFile
    add("")
    add("/* Begin PBXBuildFile section */")
    add(
        f"\t\t{source_build['RecipesExplorerApp.swift']} /* RecipesExplorerApp.swift in Sources */ = "
        f"{{isa = PBXBuildFile; fileRef = {source_refs['RecipesExplorerApp.swift']} /* RecipesExplorerApp.swift */; }};"
    )
    for path in sources[1:]:
        name = os.path.basename(path)
        add(
            f"\t\t{source_build[path]} /* {name} in Sources */ = "
            f"{{isa = PBXBuildFile; fileRef = {source_refs[path]} /* {name} */; }};"
        )
    add(
        f"\t\t{oid('build:assets')} /* Assets.xcassets in Resources */ = "
        f"{{isa = PBXBuildFile; fileRef = {assets_ref} /* Assets.xcassets */; }};"
    )
    add(
        f"\t\t{oid('build:package-product')} /* SharedRecipesSwift in Frameworks */ = "
        f"{{isa = PBXBuildFile; productRef = {product_dep} /* SharedRecipesSwift */; }};"
    )
    add(
        f"\t\t{ui_test_build} /* RecipeFlowUITests.swift in Sources */ = "
        f"{{isa = PBXBuildFile; fileRef = {ui_test_file} /* RecipeFlowUITests.swift */; }};"
    )
    add("/* End PBXBuildFile section */")

    # PBXFileReference
    add("")
    add("/* Begin PBXFileReference section */")
    add(
        f"\t\t{app_ref} /* {PROJECT_NAME}.app */ = {{isa = PBXFileReference; explicitFileType = wrapper.application; "
        f"includeInIndex = 0; path = {PROJECT_NAME}.app; sourceTree = BUILT_PRODUCTS_DIR; }};"
    )
    add(
        f"\t\t{assets_ref} /* Assets.xcassets */ = {{isa = PBXFileReference; lastKnownFileType = folder.assetcatalog; "
        f"path = Assets.xcassets; sourceTree = \"<group>\"; }};"
    )
    add(
        f"\t\t{config_ref} /* Config.xcconfig */ = {{isa = PBXFileReference; lastKnownFileType = text.xcconfig; "
        f"path = Configuration/Config.xcconfig; sourceTree = \"<group>\"; }};"
    )
    add(
        f"\t\t{info_ref} /* Info.plist */ = {{isa = PBXFileReference; lastKnownFileType = text.plist.xml; "
        f"path = Info.plist; sourceTree = \"<group>\"; }};"
    )
    for path in sources:
        name = os.path.basename(path)
        add(
            f"\t\t{source_refs[path]} /* {name} */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; "
            f"path = {os.path.basename(path)}; sourceTree = \"<group>\"; }};"
        )
    add(
        f"\t\t{package_ref} /* myApp */ = {{isa = PBXFileReference; lastKnownFileType = wrapper; name = MyApplication; "
        f"path = ..; sourceTree = \"<group>\"; }};"
    )
    add(
        f"\t\t{ui_test_product} /* {PROJECT_NAME}UITests.xctest */ = {{isa = PBXFileReference; "
        f"explicitFileType = wrapper.cfbundle; includeInIndex = 0; path = {PROJECT_NAME}UITests.xctest; "
        f"sourceTree = BUILT_PRODUCTS_DIR; }};"
    )
    add(
        f"\t\t{ui_test_file} /* RecipeFlowUITests.swift */ = {{isa = PBXFileReference; "
        f"lastKnownFileType = sourcecode.swift; path = RecipeFlowUITests.swift; sourceTree = \"<group>\"; }};"
    )
    add(
        f"\t\t{ui_test_info} /* Info.plist */ = {{isa = PBXFileReference; lastKnownFileType = text.plist.xml; "
        f"path = Info.plist; sourceTree = \"<group>\"; }};"
    )
    add("/* End PBXFileReference section */")

    # PBXFrameworksBuildPhase
    add("")
    add("/* Begin PBXFrameworksBuildPhase section */")
    add(f"\t\t{frameworks_phase} /* Frameworks */ = {{")
    add("\t\t\tisa = PBXFrameworksBuildPhase;")
    add("\t\t\tbuildActionMask = 2147483647;")
    add("\t\t\tfiles = (")
    add(f"\t\t\t\t{oid('build:package-product')} /* SharedRecipesSwift in Frameworks */,")
    add("\t\t\t);")
    add("\t\t\trunOnlyForDeploymentPostprocessing = 0;")
    add("\t\t};")
    add("/* End PBXFrameworksBuildPhase section */")

    # PBXGroup
    add("")
    add("/* Begin PBXGroup section */")
    add(f"\t\t{main_group} = {{")
    add("\t\t\tisa = PBXGroup;")
    add("\t\t\tchildren = (")
    add(f"\t\t\t\t{app_group} /* {PROJECT_NAME} */,")
    add(f"\t\t\t\t{ui_test_group} /* {PROJECT_NAME}UITests */,")
    add(f"\t\t\t\t{config_ref} /* Config.xcconfig */,")
    add(f"\t\t\t\t{frameworks_group} /* Frameworks */,")
    add(f"\t\t\t\t{products_group} /* Products */,")
    add("\t\t\t);")
    add("\t\t\tsourceTree = \"<group>\";")
    add("\t\t};")
    add(f"\t\t{app_group} /* {PROJECT_NAME} */ = {{")
    add("\t\t\tisa = PBXGroup;")
    add("\t\t\tchildren = (")
    for name, paths in SOURCE_GROUPS.items():
        add(f"\t\t\t\t{group_ids[name]} /* {name} */,")
    add(f"\t\t\t\t{info_ref} /* Info.plist */,")
    add(f"\t\t\t\t{assets_ref} /* Assets.xcassets */,")
    add("\t\t\t);")
    add(f"\t\t\tpath = {PROJECT_NAME};")
    add("\t\t\tsourceTree = \"<group>\";")
    add("\t\t};")
    for name, paths in SOURCE_GROUPS.items():
        add(f"\t\t{group_ids[name]} /* {name} */ = {{")
        add("\t\t\tisa = PBXGroup;")
        add("\t\t\tchildren = (")
        for path in paths:
            add(f"\t\t\t\t{source_refs[path]} /* {os.path.basename(path)} */,")
        add("\t\t\t);")
        add(f"\t\t\tpath = {name};")
        add("\t\t\tsourceTree = \"<group>\";")
        add("\t\t};")
    add(f"\t\t{frameworks_group} /* Frameworks */ = {{")
    add("\t\t\tisa = PBXGroup;")
    add("\t\t\tchildren = (")
    add(f"\t\t\t\t{package_ref} /* myApp */,")
    add("\t\t\t);")
    add("\t\t\tname = Frameworks;")
    add("\t\t\tsourceTree = \"<group>\";")
    add("\t\t};")
    add(f"\t\t{ui_test_group} /* {PROJECT_NAME}UITests */ = {{")
    add("\t\t\tisa = PBXGroup;")
    add("\t\t\tchildren = (")
    add(f"\t\t\t\t{ui_test_file} /* RecipeFlowUITests.swift */,")
    add(f"\t\t\t\t{ui_test_info} /* Info.plist */,")
    add("\t\t\t);")
    add(f"\t\t\tpath = {PROJECT_NAME}UITests;")
    add("\t\t\tsourceTree = \"<group>\";")
    add("\t\t};")
    add(f"\t\t{products_group} /* Products */ = {{")
    add("\t\t\tisa = PBXGroup;")
    add("\t\t\tchildren = (")
    add(f"\t\t\t\t{app_ref} /* {PROJECT_NAME}.app */,")
    add(f"\t\t\t\t{ui_test_product} /* {PROJECT_NAME}UITests.xctest */,")
    add("\t\t\t);")
    add("\t\t\tname = Products;")
    add("\t\t\tsourceTree = \"<group>\";")
    add("\t\t};")
    add("/* End PBXGroup section */")

    # PBXNativeTarget
    add("")
    add("/* Begin PBXNativeTarget section */")
    add(f"\t\t{target} /* {PROJECT_NAME} */ = {{")
    add("\t\t\tisa = PBXNativeTarget;")
    add(f"\t\t\tbuildConfigurationList = {config_list_target};")
    add("\t\t\tbuildPhases = (")
    add(f"\t\t\t\t{sources_phase} /* Sources */,")
    add(f"\t\t\t\t{frameworks_phase} /* Frameworks */,")
    add(f"\t\t\t\t{resources_phase} /* Resources */,")
    add("\t\t\t);")
    add("\t\t\tbuildRules = (")
    add("\t\t\t);")
    add("\t\t\tdependencies = (")
    add("\t\t\t);")
    add(f"\t\t\tname = {PROJECT_NAME};")
    add(f"\t\t\tpackageProductDependencies = (")
    add(f"\t\t\t\t{product_dep} /* SharedRecipesSwift */,")
    add("\t\t\t);")
    add(f"\t\t\tproductName = {PROJECT_NAME};")
    add(f"\t\t\tproductReference = {app_ref} /* {PROJECT_NAME}.app */;")
    add("\t\t\tproductType = \"com.apple.product-type.application\";")
    add("\t\t};")
    add(f"\t\t{ui_test_target} /* {PROJECT_NAME}UITests */ = {{")
    add("\t\t\tisa = PBXNativeTarget;")
    add(f"\t\t\tbuildConfigurationList = {config_list_uitest};")
    add("\t\t\tbuildPhases = (")
    add(f"\t\t\t\t{ui_test_sources} /* Sources */,")
    add(f"\t\t\t\t{ui_test_frameworks} /* Frameworks */,")
    add(f"\t\t\t\t{ui_test_resources} /* Resources */,")
    add("\t\t\t);")
    add("\t\t\tbuildRules = (")
    add("\t\t\t);")
    add("\t\t\tdependencies = (")
    add(f"\t\t\t\t{ui_test_dependency} /* PBXTargetDependency */,")
    add("\t\t\t);")
    add(f"\t\t\tname = {PROJECT_NAME}UITests;")
    add(f"\t\t\tproductName = {PROJECT_NAME}UITests;")
    add(f"\t\t\tproductReference = {ui_test_product} /* {PROJECT_NAME}UITests.xctest */;")
    add("\t\t\tproductType = \"com.apple.product-type.bundle.ui-testing\";")
    add("\t\t};")
    add("/* End PBXNativeTarget section */")

    add("")
    add("/* Begin PBXTargetDependency section */")
    add(f"\t\t{ui_test_dependency} /* PBXTargetDependency */ = {{")
    add("\t\t\tisa = PBXTargetDependency;")
    add(f"\t\t\ttarget = {target} /* {PROJECT_NAME} */;")
    add(f"\t\t\ttargetProxy = {ui_test_proxy} /* PBXContainerItemProxy */;")
    add("\t\t};")
    add("/* End PBXTargetDependency section */")

    add("")
    add("/* Begin PBXContainerItemProxy section */")
    add(f"\t\t{ui_test_proxy} /* PBXContainerItemProxy */ = {{")
    add("\t\t\tisa = PBXContainerItemProxy;")
    add(f"\t\t\tcontainerPortal = {project} /* Project object */;")
    add("\t\t\tproxyType = 1;")
    add(f"\t\t\tremoteGlobalIDString = {target};")
    add(f"\t\t\tremoteInfo = {PROJECT_NAME};")
    add("\t\t};")
    add("/* End PBXContainerItemProxy section */")

    # PBXProject
    add("")
    add("/* Begin PBXProject section */")
    add(f"\t\t{project} /* Project object */ = {{")
    add("\t\t\tisa = PBXProject;")
    add("\t\t\tattributes = {")
    add("\t\t\t\tBuildIndependentTargetsInParallel = 1;")
    add("\t\t\t\tLastSwiftUpdateCheck = 1600;")
    add("\t\t\t\tLastUpgradeCheck = 1600;")
    add("\t\t\t\tTargetAttributes = {")
    add(f"\t\t\t\t\t{target} = {{")
    add("\t\t\t\t\t\tCreatedOnToolsVersion = 16.0;")
    add("\t\t\t\t\t};")
    add(f"\t\t\t\t\t{ui_test_target} = {{")
    add("\t\t\t\t\t\tCreatedOnToolsVersion = 16.0;")
    add(f"\t\t\t\t\t\tTestTargetID = {target};")
    add("\t\t\t\t\t};")
    add("\t\t\t\t};")
    add("\t\t\t};")
    add(f"\t\t\tbuildConfigurationList = {config_list_project};")
    add("\t\t\tdevelopmentRegion = en;")
    add("\t\t\thasScannedForEncodings = 0;")
    add("\t\t\tknownRegions = (")
    add("\t\t\t\ten,")
    add("\t\t\t\tBase,")
    add("\t\t\t);")
    add(f"\t\t\tmainGroup = {main_group};")
    add(f"\t\t\tpackageReferences = (")
    add(f"\t\t\t\t{package_dep} /* XCLocalSwiftPackageReference \"..\" */,")
    add("\t\t\t);")
    add(f"\t\t\tproductRefGroup = {products_group} /* Products */;")
    add("\t\t\tprojectDirPath = \"\";")
    add("\t\t\tprojectRoot = \"\";")
    add("\t\t\ttargets = (")
    add(f"\t\t\t\t{target} /* {PROJECT_NAME} */,")
    add(f"\t\t\t\t{ui_test_target} /* {PROJECT_NAME}UITests */,")
    add("\t\t\t);")
    add("\t\t};")
    add("/* End PBXProject section */")

    # PBXResourcesBuildPhase
    add("")
    add("/* Begin PBXResourcesBuildPhase section */")
    add(f"\t\t{resources_phase} /* Resources */ = {{")
    add("\t\t\tisa = PBXResourcesBuildPhase;")
    add("\t\t\tbuildActionMask = 2147483647;")
    add("\t\t\tfiles = (")
    add(f"\t\t\t\t{oid('build:assets')} /* Assets.xcassets in Resources */,")
    add("\t\t\t);")
    add("\t\t\trunOnlyForDeploymentPostprocessing = 0;")
    add("\t\t};")
    add("/* End PBXResourcesBuildPhase section */")

    # PBXSourcesBuildPhase
    add("")
    add("/* Begin PBXSourcesBuildPhase section */")
    add(f"\t\t{sources_phase} /* Sources */ = {{")
    add("\t\t\tisa = PBXSourcesBuildPhase;")
    add("\t\t\tbuildActionMask = 2147483647;")
    add("\t\t\tfiles = (")
    for path in sources:
        add(f"\t\t\t\t{source_build[path]} /* {os.path.basename(path)} in Sources */,")
    add("\t\t\t);")
    add("\t\t\trunOnlyForDeploymentPostprocessing = 0;")
    add("\t\t};")
    add("/* End PBXSourcesBuildPhase section */")

    add("")
    add("/* Begin PBXFrameworksBuildPhase section for the UI tests */")
    add(f"\t\t{ui_test_frameworks} /* Frameworks */ = {{")
    add("\t\t\tisa = PBXFrameworksBuildPhase;")
    add("\t\t\tbuildActionMask = 2147483647;")
    add("\t\t\tfiles = (")
    add("\t\t\t);")
    add("\t\t\trunOnlyForDeploymentPostprocessing = 0;")
    add("\t\t};")
    add(f"\t\t{ui_test_resources} /* Resources */ = {{")
    add("\t\t\tisa = PBXResourcesBuildPhase;")
    add("\t\t\tbuildActionMask = 2147483647;")
    add("\t\t\tfiles = (")
    add("\t\t\t);")
    add("\t\t\trunOnlyForDeploymentPostprocessing = 0;")
    add("\t\t};")
    add("/* End UI test build phases */")

    add("")
    add("/* Begin PBXSourcesBuildPhase for the UI tests */")
    add(f"\t\t{ui_test_sources} /* Sources */ = {{")
    add("\t\t\tisa = PBXSourcesBuildPhase;")
    add("\t\t\tbuildActionMask = 2147483647;")
    add("\t\t\tfiles = (")
    add(f"\t\t\t\t{ui_test_build} /* RecipeFlowUITests.swift in Sources */,")
    add("\t\t\t);")
    add("\t\t\trunOnlyForDeploymentPostprocessing = 0;")
    add("\t\t};")
    add("/* End UI test sources build phase */")

    # XCBuildConfiguration
    add("")
    add("/* Begin XCBuildConfiguration section */")
    add(f"\t\t{config_debug_project} /* Debug */ = {{")
    add("\t\t\tisa = XCBuildConfiguration;")
    add("\t\t\tbuildSettings = {")
    add(build_settings_project(True))
    add("\t\t\t};")
    add("\t\t\tname = Debug;")
    add("\t\t};")
    add(f"\t\t{config_release_project} /* Release */ = {{")
    add("\t\t\tisa = XCBuildConfiguration;")
    add("\t\t\tbuildSettings = {")
    add(build_settings_project(False))
    add("\t\t\t};")
    add("\t\t\tname = Release;")
    add("\t\t};")
    add(f"\t\t{config_debug_target} /* Debug */ = {{")
    add("\t\t\tisa = XCBuildConfiguration;")
    add(f"\t\t\tbaseConfigurationReference = {config_ref} /* Config.xcconfig */;")
    add("\t\t\tbuildSettings = {")
    add(build_settings_target(True))
    add("\t\t\t};")
    add("\t\t\tname = Debug;")
    add("\t\t};")
    add(f"\t\t{config_release_target} /* Release */ = {{")
    add("\t\t\tisa = XCBuildConfiguration;")
    add(f"\t\t\tbaseConfigurationReference = {config_ref} /* Config.xcconfig */;")
    add("\t\t\tbuildSettings = {")
    add(build_settings_target(False))
    add("\t\t\t};")
    add("\t\t\tname = Release;")
    add("\t\t};")
    for (identifier, name, debug) in (
        (config_debug_uitest, "Debug", True),
        (config_release_uitest, "Release", False),
    ):
        add(f"\t\t{identifier} /* {name} */ = {{")
        add("\t\t\tisa = XCBuildConfiguration;")
        add("\t\t\tbuildSettings = {")
        add("\t\t\t\tCODE_SIGNING_ALLOWED = NO;")
        add("\t\t\t\tCODE_SIGNING_REQUIRED = NO;")
        add("\t\t\t\tCODE_SIGN_STYLE = Automatic;")
        add(f"\t\t\t\tCURRENT_PROJECT_VERSION = 1;")
        add("\t\t\t\tGENERATE_INFOPLIST_FILE = NO;")
        add(f"\t\t\t\tINFOPLIST_FILE = \"{PROJECT_NAME}UITests/Info.plist\";")
        add(f"\t\t\t\tIPHONEOS_DEPLOYMENT_TARGET = {DEPLOYMENT_TARGET};")
        add("\t\t\t\tMARKETING_VERSION = 1.0;")
        add(f"\t\t\t\tPRODUCT_BUNDLE_IDENTIFIER = {BUNDLE_ID}.uitests;")
        add("\t\t\t\tPRODUCT_NAME = \"$(TARGET_NAME)\";")
        add(f"\t\t\t\tSWIFT_VERSION = {SWIFT_VERSION};")
        add("\t\t\t\tTARGETED_DEVICE_FAMILY = \"1,2\";")
        add(f"\t\t\t\tTEST_TARGET_NAME = {PROJECT_NAME};")
        add("\t\t\t};")
        add(f"\t\t\tname = {name};")
        add("\t\t};")
    add("/* End XCBuildConfiguration section */")

    # XCConfigurationList
    add("")
    add("/* Begin XCConfigurationList section */")
    add(f"\t\t{config_list_project} /* Build configuration list for PBXProject \"{PROJECT_NAME}\" */ = {{")
    add("\t\t\tisa = XCConfigurationList;")
    add("\t\t\tbuildConfigurations = (")
    add(f"\t\t\t\t{config_debug_project} /* Debug */,")
    add(f"\t\t\t\t{config_release_project} /* Release */,")
    add("\t\t\t);")
    add("\t\t\tdefaultConfigurationIsVisible = 0;")
    add("\t\t\tdefaultConfigurationName = Release;")
    add("\t\t};")
    add(f"\t\t{config_list_target} /* Build configuration list for PBXNativeTarget \"{PROJECT_NAME}\" */ = {{")
    add("\t\t\tisa = XCConfigurationList;")
    add("\t\t\tbuildConfigurations = (")
    add(f"\t\t\t\t{config_debug_target} /* Debug */,")
    add(f"\t\t\t\t{config_release_target} /* Release */,")
    add("\t\t\t);")
    add("\t\t\tdefaultConfigurationIsVisible = 0;")
    add("\t\t\tdefaultConfigurationName = Release;")
    add("\t\t};")
    add(f"\t\t{config_list_uitest} /* Build configuration list for PBXNativeTarget \"{PROJECT_NAME}UITests\" */ = {{")
    add("\t\t\tisa = XCConfigurationList;")
    add("\t\t\tbuildConfigurations = (")
    add(f"\t\t\t\t{config_debug_uitest} /* Debug */,")
    add(f"\t\t\t\t{config_release_uitest} /* Release */,")
    add("\t\t\t);")
    add("\t\t\tdefaultConfigurationIsVisible = 0;")
    add("\t\t\tdefaultConfigurationName = Release;")
    add("\t\t};")
    add("/* End XCConfigurationList section */")

    # XCLocalSwiftPackageReference + XCSwiftPackageProductDependency
    add("")
    add("/* Begin XCLocalSwiftPackageReference section */")
    add(f"\t\t{package_dep} /* XCLocalSwiftPackageReference \"..\" */ = {{")
    add("\t\t\tisa = XCLocalSwiftPackageReference;")
    add("\t\t\trelativePath = ..;")
    add("\t\t};")
    add("/* End XCLocalSwiftPackageReference section */")
    add("")
    add("/* Begin XCSwiftPackageProductDependency section */")
    add(f"\t\t{product_dep} /* SharedRecipesSwift */ = {{")
    add("\t\t\tisa = XCSwiftPackageProductDependency;")
    add(f"\t\t\tpackage = {package_dep} /* XCLocalSwiftPackageReference \"..\" */;")
    add("\t\t\tproductName = SharedRecipesSwift;")
    add("\t\t};")
    add("/* End XCSwiftPackageProductDependency section */")

    add("\t};")
    add(f"\trootObject = {project} /* Project object */;")
    add("}")

    (XCODEPROJ / "project.pbxproj").parent.mkdir(parents=True, exist_ok=True)
    (XCODEPROJ / "project.pbxproj").write_text("\n".join(lines) + "\n")

    # Shared scheme so `xcodebuild -scheme RecipesExplorer` works out of the box.
    scheme_dir = XCODEPROJ / "xcshareddata" / "xcschemes"
    scheme_dir.mkdir(parents=True, exist_ok=True)
    (scheme_dir / f"{PROJECT_NAME}.xcscheme").write_text(
        f"""<?xml version="1.0" encoding="UTF-8"?>
<Scheme LastUpgradeVersion = "1600" version = "1.7">
   <BuildAction parallelizeBuildables = "YES" buildImplicitDependencies = "YES">
      <BuildActionEntries>
         <BuildActionEntry buildForTesting = "YES" buildForRunning = "YES" buildForProfiling = "YES" buildForArchiving = "YES" buildForAnalyzing = "YES">
            <BuildableReference
               BuildableIdentifier = "primary"
               BlueprintIdentifier = "{target}"
               BuildableName = "{PROJECT_NAME}.app"
               BlueprintName = "{PROJECT_NAME}"
               ReferencedContainer = "container:{PROJECT_NAME}.xcodeproj">
            </BuildableReference>
         </BuildActionEntry>
      </BuildActionEntries>
   </BuildAction>
   <TestAction buildConfiguration = "Debug" selectedDebuggerIdentifier = "Xcode.DebuggerFoundation.Debugger.LLDB" selectedLauncherIdentifier = "Xcode.DebuggerFoundation.Launcher.LLDB" shouldUseLaunchSchemeArgsEnv = "YES">
      <Testables>
         <TestableReference skipped = "NO">
            <BuildableReference
               BuildableIdentifier = "primary"
               BlueprintIdentifier = "{ui_test_target}"
               BuildableName = "{PROJECT_NAME}UITests.xctest"
               BlueprintName = "{PROJECT_NAME}UITests"
               ReferencedContainer = "container:{PROJECT_NAME}.xcodeproj">
            </BuildableReference>
         </TestableReference>
      </Testables>
   </TestAction>
   <LaunchAction buildConfiguration = "Debug" selectedDebuggerIdentifier = "Xcode.DebuggerFoundation.Debugger.LLDB" selectedLauncherIdentifier = "Xcode.DebuggerFoundation.Launcher.LLDB" launchStyle = "0" useCustomWorkingDirectory = "NO" ignoresPersistentStateOnLaunch = "NO" debugDocumentVersioning = "YES" debugServiceExtension = "internal" allowLocationSimulation = "YES">
      <BuildableProductRunnable runnableDebuggingMode = "0">
         <BuildableReference
            BuildableIdentifier = "primary"
            BlueprintIdentifier = "{target}"
            BuildableName = "{PROJECT_NAME}.app"
            BlueprintName = "{PROJECT_NAME}"
            ReferencedContainer = "container:{PROJECT_NAME}.xcodeproj">
         </BuildableReference>
      </BuildableProductRunnable>
   </LaunchAction>
   <ProfileAction buildConfiguration = "Release" shouldUseLaunchSchemeArgsEnv = "YES" savedToolIdentifier = "" useCustomWorkingDirectory = "NO" debugDocumentVersioning = "YES">
      <BuildableProductRunnable runnableDebuggingMode = "0">
         <BuildableReference
            BuildableIdentifier = "primary"
            BlueprintIdentifier = "{target}"
            BuildableName = "{PROJECT_NAME}.app"
            BlueprintName = "{PROJECT_NAME}"
            ReferencedContainer = "container:{PROJECT_NAME}.xcodeproj">
         </BuildableReference>
      </BuildableProductRunnable>
   </ProfileAction>
   <AnalyzeAction buildConfiguration = "Debug">
   </AnalyzeAction>
   <ArchiveAction buildConfiguration = "Release" revealArchiveInOrganizer = "YES">
   </ArchiveAction>
</Scheme>
"""
    )

    print(f"wrote {XCODEPROJ / 'project.pbxproj'}")
    print(f"wrote {scheme_dir / (PROJECT_NAME + '.xcscheme')}")


if __name__ == "__main__":
    main()

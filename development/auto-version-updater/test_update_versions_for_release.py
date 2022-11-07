#!/usr/bin/env python3
#
# Copyright (C) 2020 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import unittest
import os
from update_versions_for_release import *
from shutil import rmtree

class TestVersionUpdates(unittest.TestCase):

    def test_increment_version(self):
        new_version = increment_version("1.0.0-alpha01")
        self.assertEqual("1.0.0-alpha02", new_version)

        new_version = increment_version("1.1.0-alpha01")
        self.assertEqual("1.1.0-alpha02", new_version)

        new_version = increment_version("1.0.0-alpha19")
        self.assertEqual("1.0.0-alpha20", new_version)

        new_version = increment_version("1.0.0-rc01")
        self.assertEqual("1.1.0-alpha01", new_version)

        new_version = increment_version("1.3.0-beta02")
        self.assertEqual("1.3.0-beta03", new_version)

        new_version = increment_version("1.0.1")
        self.assertEqual("1.1.0-alpha01", new_version)

    def test_increment_version_within_minor_version(self):
        new_version = increment_version_within_minor_version("1.0.0-alpha01")
        self.assertEqual("1.0.0-alpha02", new_version)

        new_version = increment_version_within_minor_version("1.1.0-alpha01")
        self.assertEqual("1.1.0-alpha02", new_version)

        new_version = increment_version_within_minor_version("1.0.0-alpha19")
        self.assertEqual("1.0.0-alpha20", new_version)

        new_version = increment_version_within_minor_version("1.0.0-rc01")
        self.assertEqual("1.0.0-rc02", new_version)

        new_version = increment_version_within_minor_version("1.3.0-beta02")
        self.assertEqual("1.3.0-beta03", new_version)

        new_version = increment_version_within_minor_version("1.0.1")
        self.assertEqual("1.0.2", new_version)

    def test_get_higher_version(self):
        higher_version = get_higher_version("1.0.0-alpha01", "1.0.0-alpha02")
        self.assertEqual("1.0.0-alpha02", higher_version)

        higher_version = get_higher_version("1.0.0-alpha02", "1.0.0-alpha01")
        self.assertEqual("1.0.0-alpha02", higher_version)

        higher_version = get_higher_version("1.0.0-alpha02", "1.0.0-alpha02")
        self.assertEqual("1.0.0-alpha02", higher_version)

        higher_version = get_higher_version("1.1.0-alpha01", "1.0.0-alpha02")
        self.assertEqual("1.1.0-alpha01", higher_version)

        higher_version = get_higher_version("1.0.0-rc05", "1.2.0-beta02")
        self.assertEqual("1.2.0-beta02", higher_version)

        higher_version = get_higher_version("1.3.0-beta01", "1.5.0-beta01")
        self.assertEqual("1.5.0-beta01", higher_version)

        higher_version = get_higher_version("3.0.0-alpha01", "1.0.0-alpha02")
        self.assertEqual("3.0.0-alpha01", higher_version)

        higher_version = get_higher_version("1.0.0-beta01", "1.0.0-rc01")
        self.assertEqual("1.0.0-rc01", higher_version)

        higher_version = get_higher_version("1.4.0-beta01", "1.0.2")
        self.assertEqual("1.4.0-beta01", higher_version)

        higher_version = get_higher_version("1.4.0-beta01", "1.4.2")
        self.assertEqual("1.4.2", higher_version)

        higher_version = get_higher_version("1.4.0", "1.4.2")
        self.assertEqual("1.4.2", higher_version)

    def test_should_update_group_version_in_library_versions_toml(self):
        self.assertTrue(should_update_group_version_in_library_versions_toml(
            "1.1.0-alpha01", "1.1.0-alpha02", "androidx.core"))
        self.assertTrue(should_update_group_version_in_library_versions_toml(
            "1.1.0-alpha01", "1.3.0-alpha01", "androidx.appcompat"))
        self.assertFalse(should_update_group_version_in_library_versions_toml(
            "1.1.0-alpha01", "1.0.0-alpha01", "androidx.work"))

        self.assertTrue(should_update_group_version_in_library_versions_toml(
            "1.0.0-beta04", "1.1.0-alpha02", "androidx.wear"))
        self.assertTrue(should_update_group_version_in_library_versions_toml(
            "1.0.0-beta04", "1.3.0-alpha01", "androidx.viewpager"))
        self.assertFalse(should_update_group_version_in_library_versions_toml(
            "1.0.0-beta04", "1.0.0-alpha01", "androidx.compose.foundation"))

        self.assertTrue(should_update_group_version_in_library_versions_toml(
            "1.0.0-beta04", "1.1.0-alpha02", "androidx.tracing"))
        self.assertTrue(should_update_group_version_in_library_versions_toml(
            "1.0.0-beta04", "1.3.0-alpha01", "androidx.tracing"))
        self.assertTrue(should_update_group_version_in_library_versions_toml(
            "1.2.0", "1.3.0-alpha01", "androidx.tracing"))

        self.assertFalse(should_update_group_version_in_library_versions_toml(
            "1.0.0-beta04", "1.1.0-alpha02", "androidx.car"))
        self.assertFalse(should_update_group_version_in_library_versions_toml(
            "1.0.0-beta04", "1.3.0-alpha01", "androidx.car"))
        self.assertFalse(should_update_group_version_in_library_versions_toml(
            "1.0.0-beta04", "1.0.0-alpha01", "androidx.car"))

        self.assertFalse(should_update_group_version_in_library_versions_toml(
            "1.0.0-beta04", "1.1.0-alpha02", "androidx.compose.compiler"))
        self.assertFalse(should_update_group_version_in_library_versions_toml(
            "1.0.0-beta04", "1.3.0-alpha01", "androidx.compose.compiler"))
        self.assertFalse(should_update_group_version_in_library_versions_toml(
            "1.2.0", "1.3.0-alpha01", "androidx.compose.compiler"))

    def test_should_update_artifact_version_in_library_versions_toml(self):
        self.assertTrue(should_update_artifact_version_in_library_versions_toml(
            "1.1.0-alpha01", "1.1.0-alpha02", "core"))
        self.assertTrue(should_update_artifact_version_in_library_versions_toml(
            "1.1.0-alpha01", "1.3.0-alpha01", "appcompat"))
        self.assertFalse(should_update_artifact_version_in_library_versions_toml(
            "1.1.0-alpha01", "1.0.0-alpha01", "work-runtime"))

        self.assertTrue(should_update_artifact_version_in_library_versions_toml(
            "1.0.0-beta04", "1.1.0-alpha02", "tracing-perfetto"))
        self.assertTrue(should_update_artifact_version_in_library_versions_toml(
            "1.0.0-beta04", "1.3.0-alpha01", "tracing-perfetto"))
        self.assertFalse(should_update_artifact_version_in_library_versions_toml(
            "1.0.0-beta04", "1.0.0-alpha01", "tracing-perfetto"))
        self.assertTrue(should_update_artifact_version_in_library_versions_toml(
            "1.1.0-alpha02", "1.1.0-alpha03", "tracing-perfetto"))
        self.assertTrue(should_update_artifact_version_in_library_versions_toml(
            "1.1.0-alpha02", "1.1.0-alpha03", "tracing-perfetto-binary"))
        self.assertTrue(should_update_artifact_version_in_library_versions_toml(
            "1.1.0-alpha02", "1.1.0-alpha03", "tracing-perfetto-common"))

    def test_get_library_constants_in_library_versions_toml(self):
        self.assertEqual(
            get_library_constants_in_library_versions_toml("androidx.foo", "foo"),
            ("FOO", "FOO"))
        self.assertEqual(
            get_library_constants_in_library_versions_toml("androidx.foo.bar", "bar-qux"),
            ("FOO_BAR", "BAR_QUX"))
        self.assertEqual(
            get_library_constants_in_library_versions_toml("androidx.foo", "foo-ktx"),
            ("FOO", "FOO_KTX"))
        self.assertEqual(
            get_library_constants_in_library_versions_toml("androidx.compose.runtime", "runtime"),
            ("COMPOSE", "RUNTIME"))
        self.assertEqual(
            get_library_constants_in_library_versions_toml("androidx.compose.runtime", "runtime-tracing"),
            ("COMPOSE_RUNTIME_TRACING", "COMPOSE_RUNTIME_TRACING"))
        self.assertEqual(
            get_library_constants_in_library_versions_toml("androidx.compose.material3", "material3"),
            ("COMPOSE_MATERIAL3", "MATERIAL3"))


class TestFileParsing(unittest.TestCase):

    def test_get_compose_to_runtime_version_map(self):
        compose_to_runtime_version_map = {}
        get_compose_to_runtime_version_map(compose_to_runtime_version_map)
        self.assertTrue("1.0.0" in compose_to_runtime_version_map.keys())
        self.assertTrue("1.0.5" in compose_to_runtime_version_map.keys())
        self.assertTrue("1.1.0-alpha05" in compose_to_runtime_version_map.keys())
        self.assertEqual(3300, compose_to_runtime_version_map["1.0.0"]["runtime_version"])
        self.assertEqual(3305, compose_to_runtime_version_map["1.0.5"]["runtime_version"])
        self.assertEqual(4400, compose_to_runtime_version_map["1.1.0-alpha05"]["runtime_version"])


class TestReplacements(unittest.TestCase):
    def test_sed(self):
        # given
        out_dir = "./out"
        test_file = out_dir + "/temp.txt"
        if not os.path.exists(out_dir):
            os.makedirs(out_dir)
        with open(test_file, "w") as f:
            f.write("ababaa\nfxfx\nbcbcbb")

        # when
        sed("[ac]", "d", test_file)

        # then
        with open(test_file) as f:
            file_contents = f.read()
        self.assertEqual("dbdbdd\nfxfx\nbdbdbb", file_contents)

        # clean-up
        if os.path.isdir(out_dir):
            rmtree(out_dir)
        elif os.path.exists(out_dir):
            os.remove(out_dir)


class TestLists(unittest.TestCase):
    def test_single_no_items(self):
        items = []
        with self.assertRaisesRegex(ValueError, '^Expected a list of size 1. Found: \\[]$'):
            single(items)

    def test_single_one_item(self):
        items = ['a']
        item = single(items)
        self.assertEqual(item, items[0])

    def test_single_multiple_items(self):
        items = ['a', 'b']
        with self.assertRaisesRegex(ValueError, '^Expected a list of size 1.'
                                                ' Found: \\[\'a\', \'b\']$'):
            single(items)


if __name__ == '__main__':
    unittest.main()

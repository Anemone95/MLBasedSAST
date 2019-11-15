#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2019 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""

import os
import platform

ROOT_DIR = os.path.split(os.path.realpath(__file__))[0]
ARCH, OS = platform.architecture()
OS = "ELF" if not OS else OS


def relative_path_from_root(path: str) -> str:
    return os.path.join(ROOT_DIR, path)


if __name__ == '__main__':
    pass

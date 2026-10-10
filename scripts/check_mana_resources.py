import argparse
from pathlib import Path
import sys
import unittest


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--allow-incomplete", action="store_true", help="Allow skips while resource files are being created")
    arguments = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    suite = unittest.defaultTestLoader.discover(str(root / "tests"), pattern="test_mana_resources.py")
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    if not result.testsRun:
        print("No mana resource checks were discovered.", file=sys.stderr)
        return 1
    if result.skipped and not arguments.allow_incomplete:
        print("Mana resource checks are incomplete; required files are still missing.", file=sys.stderr)
        return 1
    print("Resource checks only. The Java ManaMath harness is not executed by this command.")
    return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    sys.exit(main())

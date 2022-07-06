"""
command line interface
"""
from argparse import ONE_OR_MORE, ArgumentParser

from colorama import Fore

from . import __version__
from .model import User


def run():
    """
    entry point
    """
    parser = ArgumentParser(prog="helloworld", description="some documentation here")
    parser.add_argument(
        "--version", action="version", version=f"%(prog)s {__version__}"
    )
    parser.add_argument(dest="users", nargs=ONE_OR_MORE, type=User, help="your name")
    args = parser.parse_args()
    for user in args.users:
        print(f"Hello {Fore.YELLOW}{user.name}{Fore.RESET}")

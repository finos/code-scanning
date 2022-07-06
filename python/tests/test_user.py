"""
tests for user class
"""
from helloworld.model import User


def test_user():
    """
    just a test
    """
    user = User("foo")
    assert user.name == "foo"
    assert user.name_upper == "FOO"

"""
model
"""
from dataclasses import dataclass


@dataclass
class User:
    """
    just a sample model class
    """
    name: str

    @property
    def name_upper(self):
        """
        name in upper case
        """
        return self.name.upper()

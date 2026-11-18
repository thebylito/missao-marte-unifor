"""64 model rotations around screen normal; camera and illumination never move."""
import math
from nave_modelo import build_ship

def build():
    ship=build_ship()
    def animate(i):
        # Looking along +Y: negative Y rotation takes +Z (up) toward -X (left).
        ship.rotation_euler[1]=-2*math.pi*i/64
    return animate

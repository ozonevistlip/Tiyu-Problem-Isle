"""Pack the generated nine-pose source into the existing 8x9 pet atlas format."""

from math import sin, pi
from pathlib import Path

from PIL import Image, ImageChops, ImageFilter


ASSETS = Path(__file__).resolve().parents[1] / "frontend" / "src" / "assets" / "pet"
SOURCE = ASSETS / "mai-xiaowen-poses.png"
PREVIEW = ASSETS / "mai-xiaowen-preview.webp"
ATLAS = ASSETS / "mai-xiaowen-atlas.webp"
CELL_WIDTH, CELL_HEIGHT = 192, 208
ROWS, COLUMNS = 9, 8


def key_poses():
    source = Image.open(SOURCE).convert("RGBA")
    width, height = source.size
    poses = []
    for row in range(3):
        for column in range(3):
            cell = source.crop((
                column * width // 3,
                row * height // 3,
                (column + 1) * width // 3,
                (row + 1) * height // 3,
            ))
            cell = keep_main_silhouette(cell)
            bounds = cell.getchannel("A").getbbox()
            if bounds is None:
                raise ValueError(f"Empty pose at row {row}, column {column}")
            poses.append(cell.crop(bounds))
    return poses


def keep_main_silhouette(cell):
    """Discard fragments from neighboring poses at the contact-sheet seams."""
    width, height = cell.size
    alpha = cell.getchannel("A")
    pixels = bytearray(1 if value > 60 else 0 for value in alpha.getdata())
    largest = []
    for start in range(len(pixels)):
        if pixels[start] != 1:
            continue
        component = [start]
        pixels[start] = 2
        for position in component:
            x, y = position % width, position // width
            neighbors = (
                position - 1 if x else -1,
                position + 1 if x < width - 1 else -1,
                position - width if y else -1,
                position + width if y < height - 1 else -1,
            )
            for neighbor in neighbors:
                if neighbor >= 0 and pixels[neighbor] == 1:
                    pixels[neighbor] = 2
                    component.append(neighbor)
        if len(component) > len(largest):
            largest = component
    mask = bytearray(width * height)
    for position in largest:
        mask[position] = 255
    silhouette = Image.frombytes("L", (width, height), bytes(mask)).filter(ImageFilter.MaxFilter(5))
    cell.putalpha(ImageChops.multiply(alpha, silhouette))
    return cell


def fit(image, max_width, max_height):
    scale = min(max_width / image.width, max_height / image.height)
    size = (round(image.width * scale), round(image.height * scale))
    return image.resize(size, Image.Resampling.NEAREST)


def frame(pose, row, column):
    phase = 2 * pi * column / COLUMNS
    movement = sin(phase)
    scale = 1 + (0.018 if row in (0, 3, 6, 8) else 0.025) * movement
    sprite = fit(pose, round(174 * scale), round(184 * scale))
    angle = 0
    x_shift = 0
    y_shift = 0
    if row in (1, 2, 7):
        angle = round(2.5 * movement)
        x_shift = round(4 * movement)
        y_shift = round(3 * abs(movement))
    elif row == 3:
        angle = round(2 * movement)
        y_shift = round(2 * movement)
    elif row == 4:
        y_shift = -round(19 * (1 - (column - 3.5) ** 2 / 12.25))
    elif row == 5:
        angle = round(1.5 * movement)
        y_shift = round(3 * abs(movement))
    else:
        y_shift = round(2 * movement)
    if angle:
        sprite = sprite.rotate(angle, resample=Image.Resampling.NEAREST, expand=True)
    canvas = Image.new("RGBA", (CELL_WIDTH, CELL_HEIGHT), (0, 0, 0, 0))
    x = (CELL_WIDTH - sprite.width) // 2 + x_shift
    y = CELL_HEIGHT - 5 - sprite.height + y_shift
    canvas.alpha_composite(sprite, (x, y))
    return canvas


def main():
    poses = key_poses()
    preview_sprite = fit(poses[0], 448, 448)
    preview = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    preview.alpha_composite(preview_sprite, (
        (512 - preview_sprite.width) // 2,
        (512 - preview_sprite.height) // 2,
    ))
    preview.save(PREVIEW, "WEBP", lossless=True, method=6)

    atlas = Image.new("RGBA", (CELL_WIDTH * COLUMNS, CELL_HEIGHT * ROWS), (0, 0, 0, 0))
    for row, pose in enumerate(poses):
        for column in range(COLUMNS):
            atlas.alpha_composite(frame(pose, row, column),
                                  (column * CELL_WIDTH, row * CELL_HEIGHT))
    atlas.save(ATLAS, "WEBP", lossless=True, method=6)

    for path in (PREVIEW, ATLAS):
        with Image.open(path) as image:
            print(f"{path}: {image.size}, {image.mode}, {path.stat().st_size} bytes")


if __name__ == "__main__":
    main()

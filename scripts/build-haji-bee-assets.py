"""Pack Haji Bee key poses into the pet player's 8-column, 9-row WebP atlas."""

from math import pi, sin
from pathlib import Path

from PIL import Image, ImageChops, ImageFilter, ImageOps


ASSETS = Path(__file__).resolve().parents[1] / "frontend" / "src" / "assets" / "pet"
POSES = ASSETS / "haji-bee-poses.png"
RUN_CONTACT = ASSETS / "haji-bee-run-contact.png"
PREVIEW = ASSETS / "haji-bee-preview.webp"
ATLAS = ASSETS / "haji-bee-atlas.webp"
CELL_WIDTH, CELL_HEIGHT = 192, 208


def keep_main_silhouette(cell):
    """Remove stray pieces of adjacent poses at the generated sheet's seams."""
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
    bounds = cell.getchannel("A").getbbox()
    if bounds is None:
        raise ValueError("Empty key pose")
    return cell.crop(bounds)


def split_sheet(path, columns, rows):
    source = Image.open(path).convert("RGBA")
    width, height = source.size
    return [
        keep_main_silhouette(source.crop((
            column * width // columns,
            row * height // rows,
            (column + 1) * width // columns,
            (row + 1) * height // rows,
        )))
        for row in range(rows) for column in range(columns)
    ]


def fit(image, max_width, max_height):
    scale = min(max_width / image.width, max_height / image.height)
    return image.resize((round(image.width * scale), round(image.height * scale)), Image.Resampling.NEAREST)


def choose_pose(row, column, poses, contact):
    # Distinct airborne and grounded poses alternate; the boots and knees change position.
    if row == 1:
        return (poses[1], contact[0], poses[2], contact[0])[column % 4]
    if row == 2:
        return (poses[3], ImageOps.mirror(contact[0]), poses[4], ImageOps.mirror(contact[0]))[column % 4]
    if row == 7:
        return (poses[9], contact[1], poses[10], ImageOps.mirror(contact[1]))[column % 4]
    return poses[{0: 0, 3: 5, 4: 6, 5: 7, 6: 8, 8: 11}[row]]


def frame(row, column, poses, contact):
    pose = choose_pose(row, column, poses, contact)
    phase = 2 * pi * column / 8
    run = row in (1, 2, 7)
    scale = 1 + (0.012 if run else 0.02) * sin(phase)
    sprite = fit(pose, round(176 * scale), round(184 * scale))
    x_shift = round(2 * sin(phase)) if run else 0
    y_shift = -3 if run and column % 2 == 0 else 0
    if row == 4:
        y_shift = -round(18 * (1 - (column - 3.5) ** 2 / 12.25))
    elif row in (0, 3, 5, 6, 8):
        y_shift = round(2 * sin(phase))
    canvas = Image.new("RGBA", (CELL_WIDTH, CELL_HEIGHT), (0, 0, 0, 0))
    canvas.alpha_composite(sprite, (
        (CELL_WIDTH - sprite.width) // 2 + x_shift,
        CELL_HEIGHT - 5 - sprite.height + y_shift,
    ))
    return canvas


def main():
    poses = split_sheet(POSES, 4, 3)
    contact = split_sheet(RUN_CONTACT, 2, 1)

    preview_sprite = fit(poses[0], 448, 448)
    preview = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    preview.alpha_composite(preview_sprite, (
        (512 - preview_sprite.width) // 2,
        (512 - preview_sprite.height) // 2,
    ))
    preview.save(PREVIEW, "WEBP", lossless=True, method=6)

    atlas = Image.new("RGBA", (CELL_WIDTH * 8, CELL_HEIGHT * 9), (0, 0, 0, 0))
    for row in range(9):
        for column in range(8):
            atlas.alpha_composite(frame(row, column, poses, contact),
                                  (column * CELL_WIDTH, row * CELL_HEIGHT))
    atlas.save(ATLAS, "WEBP", lossless=True, method=6)

    for path in (PREVIEW, ATLAS):
        with Image.open(path) as image:
            print(f"{path}: {image.size}, {image.mode}, {path.stat().st_size} bytes")


if __name__ == "__main__":
    main()

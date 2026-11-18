#!/usr/bin/env python3
"""Carga datos de demostración en el Firebase Emulator Suite local (proyecto demo-ecomarket).

Crea cuentas en Auth, perfiles y productos en Firestore, y sube las fotos a Storage.
Solo funciona contra los emuladores: nunca toca un proyecto real de Firebase.

Uso (con los emuladores encendidos: firebase emulators:start):
    python3 tools/seed_emulator.py [--device-host 10.0.2.2]
"""
from __future__ import annotations

import argparse
import json
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

PROJECT = "demo-ecomarket"
BUCKET = f"{PROJECT}.appspot.com"
AUTH = "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1"
FIRESTORE = f"http://127.0.0.1:8080/v1/projects/{PROJECT}/databases/(default)/documents"
STORAGE = "http://127.0.0.1:9199"
OWNER = {"Authorization": "Bearer owner"}  # Credencial de administrador del emulador: omite las reglas
IMAGES = Path(__file__).parent / "seed_images"
PASSWORD = "EcoMarket2026"

USERS = [
    {"key": "admin", "name": "Laura Gómez", "email": "admin@ecomarket.co", "phone": "3000000000", "role": "ADMIN", "business": ""},
    {"key": "camila", "name": "Camila Restrepo", "email": "camila@ecomarket.co", "phone": "3001234567", "role": "SELLER", "business": "Finca La Esperanza"},
    {"key": "andres", "name": "Andrés Muñoz", "email": "andres@ecomarket.co", "phone": "3157654321", "role": "SELLER", "business": "Cooperativa Cafetera del Sur"},
    {"key": "maria", "name": "María Torres", "email": "maria@correo.com", "phone": "3109876543", "role": "BUYER", "business": ""},
    {"key": "jorge", "name": "Jorge Medina", "email": "jorge@correo.com", "phone": "3205551122", "role": "BUYER", "business": ""},
]

PRODUCTS = [
    ("aguacate", "Aguacate Hass orgánico", "Aguacate Hass de cultivo orgánico certificado, cosechado en Antioquia sin pesticidas. Bolsa de 1 kg.", 12500, 80, "Frutas", "camila", "APPROVED"),
    ("quinoa", "Quinoa real premium", "Quinoa en grano de Nariño, libre de gluten. Bolsa de 500 g.", 19500, 60, "Sin gluten", "camila", "APPROVED"),
    ("huevos", "Huevos de campo libres", "Docena de huevos de gallinas en pastoreo libre.", 15800, 40, "Orgánicos", "camila", "APPROVED"),
    ("tomate", "Tomate cherry ecológico", "Tomate cherry de invernadero agroecológico. Bandeja de 300 g.", 8900, 120, "Verduras", "camila", "APPROVED"),
    ("cafe", "Café de altura tostado", "Café de origen Huila, tostión media, 500 g. Comercio justo.", 38000, 45, "Orgánicos", "andres", "APPROVED"),
    ("miel", "Miel de abeja pura", "Miel multifloral sin procesar, frasco de vidrio de 350 g.", 24000, 30, "Orgánicos", "andres", "APPROVED"),
    ("jabon", "Jabón artesanal de lavanda", "Jabón vegano de lavanda y avena, barra de 120 g.", 11000, 70, "Vegano", "andres", "APPROVED"),
    ("mochila", "Mochila wayuu tejida", "Mochila tejida a mano por artesanas wayuu, diseño único.", 145000, 12, "Artesanías", "andres", "APPROVED"),
    ("avena", "Bebida de avena", "Bebida vegetal de avena sin azúcar añadida, 1 L.", 9800, 25, "Vegano", "camila", "PENDING"),
]


def request(method: str, url: str, body: bytes | None = None, headers: dict | None = None) -> dict:
    req = urllib.request.Request(url, data=body, method=method, headers=headers or {})
    with urllib.request.urlopen(req) as resp:
        raw = resp.read()
        return json.loads(raw) if raw else {}


def to_value(value):
    """Convierte un valor de Python al formato tipado de la API REST de Firestore."""
    if isinstance(value, bool):
        return {"booleanValue": value}
    if isinstance(value, int):
        return {"integerValue": str(value)}
    if isinstance(value, datetime):
        return {"timestampValue": value.isoformat().replace("+00:00", "Z")}
    return {"stringValue": str(value)}


def set_doc(path: str, data: dict) -> None:
    body = json.dumps({"fields": {k: to_value(v) for k, v in data.items()}}).encode()
    request("PATCH", f"{FIRESTORE}/{path}", body, {**OWNER, "Content-Type": "application/json"})


def create_user(user: dict) -> str:
    body = json.dumps({"email": user["email"], "password": PASSWORD, "displayName": user["name"],
                       "returnSecureToken": True}).encode()
    return request("POST", f"{AUTH}/accounts:signUp?key=demo", body, {"Content-Type": "application/json"})["localId"]


def upload_image(product_id: str, image: Path, device_host: str) -> tuple[str, str]:
    """Sube la foto a Storage y devuelve (url de descarga vista desde el dispositivo, ruta en el bucket)."""
    path = f"products/{product_id}/seed.jpg"
    url = f"{STORAGE}/v0/b/{BUCKET}/o?name={urllib.parse.quote(path, safe='')}"
    meta = request("POST", url, image.read_bytes(), {**OWNER, "Content-Type": "image/jpeg"})
    token = meta["downloadTokens"].split(",")[0]
    encoded = urllib.parse.quote(path, safe="")
    return f"http://{device_host}:9199/v0/b/{BUCKET}/o/{encoded}?alt=media&token={token}", path


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--device-host", default="10.0.2.2",
                        help="Host del PC visto desde el dispositivo (10.0.2.2 en el emulador de Android)")
    args = parser.parse_args()

    now = datetime.now(timezone.utc)
    uids = {}
    for user in USERS:
        uid = create_user(user)
        uids[user["key"]] = uid
        set_doc(f"users/{uid}", {"name": user["name"], "email": user["email"], "phone": user["phone"],
                                 "role": user["role"], "active": True, "business": user["business"], "createdAt": now})
        print(f"Usuario {user['role']:<6} {user['email']}")

    sellers = {u["key"]: u for u in USERS}
    for image, name, description, price, stock, category, seller, approval in PRODUCTS:
        product_id = image
        image_url, image_path = upload_image(product_id, IMAGES / f"{image}.jpg", args.device_host)
        set_doc(f"products/{product_id}", {
            "name": name, "description": description, "price": price, "stock": stock, "category": category,
            "sellerId": uids[seller], "sellerName": sellers[seller]["business"], "imageUrl": image_url,
            "imagePath": image_path, "approval": approval, "createdAt": now,
        })
        print(f"Producto {approval:<8} {name}")
    print(f"\nListo. Contraseña de todas las cuentas: {PASSWORD}")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
import base64
import datetime
from pathlib import Path
from cryptography import x509
from cryptography.x509.oid import NameOID
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.serialization import pkcs12

def generate_roxy_keystore():
    alias = b"roxypdftoolkit"
    password = b"roxy_release_pass"
    
    print("Generating 2048-bit RSA Private Key...")
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    
    subject = issuer = x509.Name([
        x509.NameAttribute(NameOID.COMMON_NAME, "Roxy PDF Toolkit"),
        x509.NameAttribute(NameOID.ORGANIZATION_NAME, "Roxy"),
        x509.NameAttribute(NameOID.ORGANIZATIONAL_UNIT_NAME, "Mobile"),
        x509.NameAttribute(NameOID.LOCALITY_NAME, "New Delhi"),
        x509.NameAttribute(NameOID.STATE_OR_PROVINCE_NAME, "Delhi"),
        x509.NameAttribute(NameOID.COUNTRY_NAME, "IN")
    ])
    
    now = datetime.datetime.now(datetime.timezone.utc)
    cert = x509.CertificateBuilder().subject_name(
        subject
    ).issuer_name(
        issuer
    ).public_key(
        key.public_key()
    ).serial_number(
        x509.random_serial_number()
    ).not_valid_before(
        now - datetime.timedelta(days=1)
    ).not_valid_after(
        now + datetime.timedelta(days=10000)
    ).sign(key, hashes.SHA256())
    
    print("Serializing PKCS12 Keystore...")
    p12_bytes = pkcs12.serialize_key_and_certificates(
        alias,
        key,
        cert,
        None,
        serialization.BestAvailableEncryption(password)
    )
    
    out_dir = Path("androidApp")
    out_dir.mkdir(parents=True, exist_ok=True)
    
    keystore_path = out_dir / "roxy.keystore"
    keystore_path.write_bytes(p12_bytes)
    
    b64_str = base64.b64encode(p12_bytes).decode("utf-8")
    b64_path = out_dir / "roxy_keystore_base64.txt"
    b64_path.write_text(b64_str, encoding="utf-8")
    
    print(f"✅ Generated: {keystore_path}")
    print(f"✅ Base64 file: {b64_path}")
    print(f"Keystore Size: {len(p12_bytes)} bytes | Base64 Length: {len(b64_str)} chars")

if __name__ == "__main__":
    generate_roxy_keystore()

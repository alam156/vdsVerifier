/* 
 * Copyright (C) 2020 Tobias Senger (info@tsenger.de)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.example.myapplication.vdstools;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPrivateKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.pmw.tinylog.Logger;

public class Signer {

	private BCRSAPrivateKey bcrsaPrivateKey;

	public Signer(PrivateKey privKey) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, NoSuchProviderException {
		Security.addProvider(new BouncyCastleProvider());
		byte[] encodedKey = privKey.getEncoded();
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
		this.bcrsaPrivateKey = (BCRSAPrivateKey) keyFactory.generatePrivate(keySpec);
	}

	public Signer(KeyStore keyStore, String keyStorePassword, String keyAlias) {
		try {
			this.bcrsaPrivateKey = (BCRSAPrivateKey) keyStore.getKey(keyAlias, keyStorePassword.toCharArray());
		} catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException e) {
			Logger.error("getPrivateKeyByAlias failed: " + e.getMessage());
		}
	}

	public int getFieldSize() {
		BigInteger modulus = this.bcrsaPrivateKey.getModulus();
		System.out.println(modulus.bitLength());
		return modulus.bitLength();
	}

	public byte[] sign(byte[] dataToSign) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException,
			InvalidAlgorithmParameterException, IOException, NoSuchProviderException {
		if (bcrsaPrivateKey == null) {
			throw new InvalidKeyException("private key not initialized. Load from file or generate new one.");
		}

		// Changed 02.12.2021:
		// Signature depends now on curves bit length according to BSI TR-03116-2
		// 2024-10-20: even more precise Doc9309-13 chapter 2.4
		int fieldBitLength = getFieldSize();
		Signature rsaSign;
		if (fieldBitLength <= 2048) {
			System.out.println("224");
			rsaSign = Signature.getInstance("SHA256withRSA", "BC");
		} else if (fieldBitLength <= 3072) {
			System.out.println("256");
			rsaSign = Signature.getInstance("SHA256withRSA", "BC");
		} else if (fieldBitLength <= 4096) {
			System.out.println("384");
			rsaSign = Signature.getInstance("SHA384withRSA", "BC");
		} else if (fieldBitLength <= 8192) {
			System.out.println("512");
			rsaSign = Signature.getInstance("SHA512withRSA", "BC");
		} else {
			Logger.error("Bit length of Field is out of defined value: " + fieldBitLength);
			throw new InvalidAlgorithmParameterException(
					"Bit length of Field is out of defined value (2048 to 8192 bits): " + fieldBitLength);
		}

		Logger.info("ECDSA algorithm: " + rsaSign.getAlgorithm());

		rsaSign.initSign(bcrsaPrivateKey);
		rsaSign.update(dataToSign);

		return rsaSign.sign();
	}
}

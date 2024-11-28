package com.example.myapplication.vdstools.vds;

import com.example.myapplication.vdstools.DataEncoder;
import com.example.myapplication.vdstools.DataParser;
import com.example.myapplication.vdstools.DerTlv;
import com.example.myapplication.vdstools.Signer;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.pmw.tinylog.Logger;


public class DigitalSeal {

	private String vdsType;
	private VdsHeader vdsHeader;
	private VdsMessage vdsMessage;
	private VdsSignature vdsSignature;

	public DigitalSeal(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
		this.vdsHeader = vdsHeader;
		this.vdsMessage = vdsMessage;
		this.vdsSignature = vdsSignature;
		this.vdsType = vdsHeader.getVdsType();
	}

	public DigitalSeal(VdsHeader vdsHeader, VdsMessage vdsMessage, Signer signer) {
		this.vdsHeader = vdsHeader;
		this.vdsMessage = vdsMessage;
		this.vdsSignature = createVdsSignature(vdsHeader, vdsMessage, signer);
		this.vdsType = vdsHeader.getVdsType();
	}

	public String getVdsType() {
		return vdsType;
	}

	public String getIssuingCountry() {
		return vdsHeader.getIssuingCountry();
	}

	/**
	 * Returns a string that identifies the signer certificate. The SignerCertRef
	 * string is build from Signer Identifier (country code || signer id) and
	 * Certificate Reference. The Signer Identifier maps to the signer certificates
	 * subject (C || CN) The Certificate Reference will be interpreted as an hex
	 * string integer that represents the serial number of the signer certificate.
	 * Leading zeros in Certificate Reference the will be cut off. e.g. Signer
	 * Identifier 'DETS' and CertificateReference '00027' will result in 'DETS27'
	 * 
	 * @return Formated SignerCertRef all UPPERCASE
	 */
	public String getSignerCertRef() {
		BigInteger certRefInteger = new BigInteger(vdsHeader.getCertificateReference(), 16);
		return String.format("%s%x", vdsHeader.getSignerIdentifier(), certRefInteger).toUpperCase();
	}

	public String getSignerIdentifier() {
		return vdsHeader.getSignerIdentifier();
	}

	public String getCertificateReference() {
		return vdsHeader.getCertificateReference();
	}

	public LocalDate getIssuingDate() {
		return vdsHeader.getIssuingDate();
	}

	public LocalDate getSigDate() {
		return vdsHeader.getSigDate();
	}

	public byte getDocFeatureRef() {
		return vdsHeader.getDocFeatureRef();
	}

	public byte getDocTypeCat() {
		return vdsHeader.getDocTypeCat();
	}

	public byte[] getHeaderAndMessageBytes() {
		return Arrays.concatenate(vdsHeader.getEncoded(), vdsMessage.getEncoded());
	}

	public byte[] getEncoded() throws IOException {
		return Arrays.concatenate(vdsHeader.getEncoded(), vdsMessage.getEncoded(), vdsSignature.getEncoded());
	}

	public byte[] getSignatureBytes() {
		return vdsSignature.getPlainSignatureBytes();
	}

	public String getRawString() throws IOException {
		return DataEncoder.encodeBase256(getEncoded());
	}

	public Map<String, Feature> getFeatureMap() {
		return vdsMessage.getFeatureMap();
	}

	public Optional<Feature> getFeature(String feature) {
		return vdsMessage.getFeature(feature);
	}

	public static DigitalSeal fromRawString(String rawString) {
		DigitalSeal seal = null;
		try {
			seal = parseVdsSeal(DataParser.decodeBase256(rawString));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return seal;
	}

	public static DigitalSeal fromByteArray(byte[] rawBytes) {
		DigitalSeal seal = null;
		try {
			seal = parseVdsSeal(rawBytes);
		} catch (IOException e) {
			Logger.error(e.getMessage());
		}
		return seal;
	}

	private static DigitalSeal parseVdsSeal(byte[] rawBytes) throws IOException {

		ByteBuffer rawData = ByteBuffer.wrap(rawBytes);
		//Logger.trace("rawData: {}", () -> Hex.toHexString(rawBytes));

		VdsHeader vdsHeader = VdsHeader.fromByteBuffer(rawData);
		VdsSignature vdsSignature = null;

		int messageStartPosition = rawData.position();

		List<DerTlv> derTlvList = DataParser
				.parseDerTLvs(Arrays.copyOfRange(rawBytes, messageStartPosition, rawBytes.length));

		List<DerTlv> featureList = new ArrayList<DerTlv>(derTlvList.size() - 1);
		//DerTlv signatureDerTLV = null;

		for (DerTlv derTlv : derTlvList) {
			if (derTlv.getTag() == (byte) 0xff) {
				//vdsSignature = VdsSignature.fromByteArray(derTlv.getEncoded());
				VdsMessage vdsMessage = new VdsMessage(vdsHeader.getVdsType(), featureList);
				return new DigitalSeal(vdsHeader, vdsMessage, VdsSignature.fromDerTlv(derTlv));
			} else {
				featureList.add(derTlv);
			}
		}
		return null;


	}

	private VdsSignature createVdsSignature(VdsHeader vdsHeader, VdsMessage vdsMessage, Signer signer) {
		byte[] headerMessage = Arrays.concatenate(vdsHeader.getEncoded(), vdsMessage.getEncoded());
		try {
			byte[] signatureBytes = signer.sign(headerMessage);
			return new VdsSignature(signatureBytes);
		} catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException
				| InvalidAlgorithmParameterException | NoSuchProviderException | IOException e) {
			Logger.error("Signature creation failed: " + e.getMessage());
			return null;
		}
	}
}
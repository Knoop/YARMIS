package com.yarmis.core.security;

public class Crypto {

	/**
	 * Encrypts the data array with the given key.
	 */
	public static byte[] encrypt(byte[] data, byte[] key) {
		// NYI;
		return data;
	}

	/**
	 * Hashes the data array, using sha256 hash.
	 */
	public static byte[] hash(byte[] data) {
		// NYI;
		return new byte[32];
	}

	/**
	 * Checks whether the signature of a given data array is valid.
	 * Note that the signature is assumed to contain the whole data
	 * array. So if your signature contains only a hash of the original
	 * data, you'll have to call this function with the hash of the data
	 * instead of the original data.
	 * @param  data      The signed data array
	 * @param  signature The signature of the data array
	 * @param  key       The key that the data array is signed with
	 * @return           True iff the signature matches the encryption of the data array with the provided key.
	 */
	public static boolean isSignatureValid(byte[] data, byte[] signature, byte[] key) {
		byte[] trueSignature = encrypt(data, key);
		if(trueSignature.length != signature.length) {
			return false;
		} else {
			for(int i = 0; i < signature.length; i++) {
				if(trueSignature[i] != signature[i]) {
					return false;
				}
			}
			return true;
		}
	}
}
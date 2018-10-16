package com.hedera.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hedera.sdk.file.HederaFile;

import java.util.HashMap;

public final class FileGetContents {

	public static HashMap<Long, byte[]> fileCache = new HashMap<>();

	public static byte[] getContents(HederaFile file) throws Exception {
		final Logger logger = LoggerFactory.getLogger(FileGetContents.class);
		
		logger.info("");
		logger.info("FILE GET CONTENTS");
		logger.info("");
		
		// run a get contents query
		byte[] contents = file.getContents();

		if (contents != null) {
			// it was successful, print it
			logger.info("===>Got contents= len: {}", contents.length);
			logger.info("===>Got contents= res: {}", file.getPrecheckResult());
			logger.info("===>Got contents= num: {}", file.fileNum);

			logger.info(new String(contents,"UTF-8"));

			if (contents.length == 0 &&
				fileCache.containsKey(file.fileNum)) {
				return fileCache.get(file.fileNum);
			}

			fileCache.put(file.fileNum, file.contents);

		} else {
			// an error occurred
			logger.info("===>Getting contents - precheck ERROR" + file.getPrecheckResult());
		}

		return file.contents;
	}
}

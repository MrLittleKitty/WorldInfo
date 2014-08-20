package com.github.coaster3000.WUUID.worlduuid;

import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class UUIDTest {

	@Test
	public void testUUID() throws IOException {
		UUID uuid = UUID.randomUUID();
		String uuidS = uuid.toString();

		long l1 = uuid.getLeastSignificantBits(), l2 = uuid.getMostSignificantBits();

		byte[] bytes = uuid.toString().getBytes("UTF-8"); //INPUT

		String s = new String(bytes, "UTF-8"); //OUTPUT
		UUID fromS = UUID.fromString(uuidS);

		UUID fromBytes = UUID.fromString(s);

		assertEquals("String uuid does not match main Java dunn broke man..", uuid, fromS);
		assertEquals(uuid, fromBytes);
	}
}
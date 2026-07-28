package com.example.xapp.common;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * UUIDv7 を採番する（docs/adr/0002）。JDK 25 の {@link UUID} は v4 しか作れないため自前で組む。
 *
 * <pre>
 *   | 48bit unix_ts_ms | 4bit ver(7) | 12bit rand_a | 2bit var | 62bit rand_b |
 * </pre>
 *
 * <p>先頭がミリ秒精度のタイムスタンプなので生成順にほぼ単調増加し、
 * B-Tree インデックスが断片化しない。かつランダム部があるため件数を推測されない。
 *
 * <p>同一ミリ秒内の順序は保証しない。カーソルページングは「同じ id より小さい」で
 * 進むため、同一ミリ秒の並びが前後しても取りこぼしや重複は起きない。
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {}

    public static UUID generate() {
        return generate(System.currentTimeMillis());
    }

    /** テストから時刻を固定できるようにした版。 */
    static UUID generate(long unixTsMillis) {
        byte[] bytes = new byte[10];
        RANDOM.nextBytes(bytes);

        long msb = (unixTsMillis & 0xFFFF_FFFF_FFFFL) << 16;
        // version = 7
        msb |= 0x7000L;
        // rand_a（12bit）
        msb |= ((long) (bytes[0] & 0x0F) << 8) | (bytes[1] & 0xFFL);

        // variant = 0b10
        long lsb = 0x8000_0000_0000_0000L;
        lsb |= ((long) (bytes[2] & 0x3F)) << 56;
        for (int i = 3; i < 10; i++) {
            lsb |= (bytes[i] & 0xFFL) << ((9 - i) * 8);
        }

        return new UUID(msb, lsb);
    }
}

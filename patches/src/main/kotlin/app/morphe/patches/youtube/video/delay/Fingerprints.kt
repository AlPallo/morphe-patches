/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.video.delay

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * Media3 MediaCodecAudioRenderer getPositionUs, the media clock that video frames are released against
 * when audio is decoded by MediaCodec.
 */
internal object MediaCodecAudioRendererPositionFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        filters = listOf(
            string("AudioCapabilities from the AudioSink are null, using default stereo output layout.")
        )
    ),
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "J",
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.IGET_WIDE),
        opcode(Opcode.RETURN_WIDE)
    )
)

/**
 * Media3 DecoderAudioRenderer getPositionUs, the media clock used when audio is decoded
 * in software, such as Opus audio decoded by LibopusAudioRenderer.
 */
internal object DecoderAudioRendererPositionFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        filters = listOf(
            string("createAudioDecoder")
        )
    ),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "J",
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.IGET_WIDE),
        opcode(Opcode.RETURN_WIDE)
    )
)

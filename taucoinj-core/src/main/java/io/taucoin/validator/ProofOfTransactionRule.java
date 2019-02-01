package io.taucoin.validator;

import io.taucoin.core.BlockHeader;
import io.taucoin.util.FastByteComparisons;

/**
 * Checks proof value against its boundary for the block header
 *
 * @author Mikhail Kalinin
 * @since 02.09.2015
 */
public class ProofOfTransactionRule extends BlockHeaderRule {

    @Override
    public boolean validate(BlockHeader header) {

        errors.clear();

        byte[] proof = header.calcPotValue();
        byte[] boundary = header.getPotBoundary();

        if (!header.isGenesis() && FastByteComparisons.compareTo(proof, 0, 32, boundary, 0, 32) > 0) {
            errors.add(String.format("#%d: proofValue > header.getPowBoundary()", header.getNumber()));
            return false;
        }

        return true;
    }
}

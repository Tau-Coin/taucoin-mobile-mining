package io.taucoin.validator;

import io.taucoin.core.BlockHeader;
import io.taucoin.validator.DependentBlockHeaderRule;

import java.math.BigInteger;

import static io.taucoin.util.BIUtil.isEqual;

/**
 * Checks block's difficulty against calculated difficulty value
 *
 * @author Mikhail Kalinin
 * @since 02.09.2015
 */
public class DifficultyRule extends DependentBlockHeaderRule {

    @Override
    public boolean validate(BlockHeader header, BlockHeader parent) {

        errors.clear();

        //TODO::b*p*t > hit
//        BigInteger calcDifficulty = header.calcDifficulty(parent);
//        BigInteger difficulty = header.getDifficultyBI();
//
//        if (!isEqual(difficulty, calcDifficulty)) {
//
//            errors.add(String.format("#%d: difficulty != calcDifficulty", header.getNumber()));
//            return false;
//        }

        return true;
    }
}

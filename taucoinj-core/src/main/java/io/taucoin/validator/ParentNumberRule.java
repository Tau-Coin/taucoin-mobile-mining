package io.taucoin.validator;

import io.taucoin.core.BlockHeader;
import io.taucoin.validator.DependentBlockHeaderRule;

/**
 *
 * @author Mikhail Kalinin
 * @since 02.09.2015
 */
public class ParentNumberRule extends DependentBlockHeaderRule {

    @Override
    public boolean validate(BlockHeader header, BlockHeader parent) {

        errors.clear();

//        if (header.getNumber() != (parent.getNumber() + 1)) {
//            errors.add(String.format("#%d: block number is not parentBlock number + 1", header.getNumber()));
//            return false;
//        }

        return true;
    }
}

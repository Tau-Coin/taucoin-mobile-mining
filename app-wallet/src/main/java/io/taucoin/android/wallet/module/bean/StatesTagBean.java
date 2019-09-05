package io.taucoin.android.wallet.module.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class StatesTagBean extends BaseBean {

    @SerializedName(value = "payload")
    private TagsBean tags;

    public TagsBean getTags() {
        return tags;
    }

    public void setTags(TagsBean tags) {
        this.tags = tags;
    }

    public static class TagsBean {

        @SerializedName(value = "startno")
        private long startNo;
        private List<String> states;
        private List<String> blocks;

        public List<String> getStates() {
            return states;
        }

        public void setStates(List<String> states) {
            this.states = states;
        }

        public List<String> getBlocks() {
            return blocks;
        }

        public void setBlocks(List<String> blocks) {
            this.blocks = blocks;
        }

        public long getStartNo() {
            return startNo;
        }

        public void setStartNo(long startNo) {
            this.startNo = startNo;
        }
    }
}
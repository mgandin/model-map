package org.modelmap.sample.field;

import static org.modelmap.sample.field.SampleTag.ACCOUNT;
import static org.modelmap.sample.field.SampleTag.READ_ONLY;
import static org.modelmap.sample.field.SampleTag.USER;

import java.util.Arrays;
import java.util.List;

import org.modelmap.core.FieldId;
import org.modelmap.core.TagId;

public enum SampleFieldId implements FieldId {

    USER_ID(USER),
    ACCOUNT_ID(ACCOUNT),

    FIRST_NAME(USER),
    LAST_NAME(USER),
    FULLNAME(USER, READ_ONLY),
    BIRTHDATE(USER),

    EMAIL(ACCOUNT),
    LANGUAGE(ACCOUNT),
    TIMEZONE(ACCOUNT),

    PHONE_NUMBER(ACCOUNT),
    EMAIL_ACCEPTED(ACCOUNT),
    EMAILS_PREFERENCES(ACCOUNT),

    FAVORITE_SITE_NAME_1(1, ACCOUNT),
    FAVORITE_SITE_NAME_2(2, ACCOUNT),
    FAVORITE_SITE_NAME_3(3, ACCOUNT),

    FAVORITE_SITE_URL_1(1, ACCOUNT),
    FAVORITE_SITE_URL_2(2, ACCOUNT),
    FAVORITE_SITE_URL_3(3, ACCOUNT),

    LOGIN(ACCOUNT),
    PASSWORD(ACCOUNT);

    private final int position;
    private final List<TagId> tags;

    SampleFieldId(TagId... tags) {
        this(-1, tags);
    }

    SampleFieldId(int position, TagId... tags) {
        this.position = position;
        this.tags = Arrays.asList(tags);
    }

    @Override
    public int position() {
        return position;
    }

    @Override
    public List<TagId> tags() {
        return tags;
    }
}

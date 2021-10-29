package com.khartec.waltz.integration_test.inmem.helpers;

import com.khartec.waltz.common.CollectionUtilities;
import com.khartec.waltz.common.DateTimeUtilities;
import com.khartec.waltz.data.measurable_category.MeasurableCategoryDao;
import com.khartec.waltz.model.measurable_category.MeasurableCategory;
import com.khartec.waltz.schema.tables.records.MeasurableCategoryRecord;
import com.khartec.waltz.schema.tables.records.MeasurableRecord;
import com.khartec.waltz.service.measurable.MeasurableService;
import com.khartec.waltz.service.measurable_category.MeasurableCategoryService;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

import static com.khartec.waltz.schema.Tables.MEASURABLE;
import static com.khartec.waltz.schema.Tables.MEASURABLE_CATEGORY;

@Service
public class MeasurableHelper {

    protected static final String LAST_UPDATE_USER = "last";
    protected static final String PROVENANCE = "test";

    @Autowired
    private MeasurableCategoryService categoryService;

    @Autowired
    private MeasurableService measurableService;

    @Autowired
    private RatingSchemeHelper ratingSchemeHelper;

    @Autowired
    private DSLContext dsl;


    public long createMeasurableCategory(String name) {
        Set<MeasurableCategory> categories = categoryService.findByExternalId(name);
        return CollectionUtilities
                .maybeFirst(categories)
                .map(c -> c.id().get())
                .orElseGet(() -> {
                    long schemeId = ratingSchemeHelper.createEmptyRatingScheme("test");
                    MeasurableCategoryRecord record = dsl.newRecord(MEASURABLE_CATEGORY);
                    record.setDescription(name);
                    record.setName(name);
                    record.setExternalId(name);
                    record.setRatingSchemeId(schemeId);
                    record.setLastUpdatedBy("admin");
                    record.setLastUpdatedAt(DateTimeUtilities.nowUtcTimestamp());
                    record.setEditable(false);
                    record.store();
                    return record.getId();
                });
    }


    public long createMeasurable(String name, long categoryId) {
        return dsl
                .select(MEASURABLE.ID)
                .from(MEASURABLE)
                .where(MEASURABLE.EXTERNAL_ID.eq(name))
                .and(MEASURABLE.MEASURABLE_CATEGORY_ID.eq(categoryId))
                .fetchOptional(MEASURABLE.ID)
                .orElseGet(() -> {
                    MeasurableRecord record = dsl.newRecord(MEASURABLE);
                    record.setMeasurableCategoryId(categoryId);
                    record.setName(name);
                    record.setDescription(name);
                    record.setConcrete(true);
                    record.setExternalId(name);
                    record.setProvenance(PROVENANCE);
                    record.setLastUpdatedBy(LAST_UPDATE_USER);
                    record.setLastUpdatedAt(DateTimeUtilities.nowUtcTimestamp());
                    record.store();
                    return record.getId();
                });
    }

}
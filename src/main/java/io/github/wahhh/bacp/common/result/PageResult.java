package io.github.wahhh.bacp.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Page wrapper aligned with MyBatis-Plus pagination metadata.
 *
 * @param <T> record type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private long total;
    private long pages;
    private long current;
    private long size;
    private List<T> records;

    /**
     * Maps a MyBatis-Plus {@link IPage} into a {@link PageResult}.
     *
     * @param page source page
     * @param <T>  record type
     * @return page DTO
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        if (page == null) {
            return PageResult.<T>builder().total(0).pages(0).current(1).size(0).records(List.of()).build();
        }
        return PageResult.<T>builder()
                .total(page.getTotal())
                .pages(page.getPages())
                .current(page.getCurrent())
                .size(page.getSize())
                .records(page.getRecords())
                .build();
    }
}

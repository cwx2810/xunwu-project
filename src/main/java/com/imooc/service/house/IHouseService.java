package com.imooc.service.house;

import com.imooc.service.ServiceMultiResult;
import com.imooc.service.ServiceResult;
import com.imooc.web.dto.HouseDTO;
import com.imooc.web.form.DatatableSearch;
import com.imooc.web.form.HouseForm;

/**
 * 房屋管理服务接口
 *
 */
public interface IHouseService {
    /**
     * 新增房源
     * @param houseForm
     * @return
     */
    ServiceResult<HouseDTO> save(HouseForm houseForm);

    /**
     * 点编辑后点更新
     * @param houseForm
     * @return
     */
    ServiceResult update(HouseForm houseForm);

    /**
     * 查找所有房源，总的表格显示
     * @param searchBody
     * @return
     */
    ServiceMultiResult<HouseDTO> adminQuery(DatatableSearch searchBody);


    /**
     * 查询完整房源信息，点编辑后显示单个房源
     * @param id
     * @return
     */
    ServiceResult<HouseDTO> findCompleteOne(Long id);
}

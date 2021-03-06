package com.imooc.web.controller.house;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import com.imooc.service.ServiceResult;
import com.imooc.web.form.RentSearch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.imooc.base.ApiResponse;
import com.imooc.base.RentValueBlock;
import com.imooc.entity.SupportAddress;
import com.imooc.service.IUserService;
import com.imooc.service.ServiceResult;
import com.imooc.service.house.IHouseService;
//import com.imooc.service.search.HouseBucketDTO;
//import com.imooc.service.search.ISearchService;
import com.imooc.web.dto.HouseDTO;
import com.imooc.web.dto.SubwayDTO;
import com.imooc.service.ServiceMultiResult;
import com.imooc.service.house.IAddressService;
import com.imooc.web.dto.SubwayStationDTO;
import com.imooc.web.dto.SupportAddressDTO;
import com.imooc.web.dto.UserDTO;
import com.imooc.web.form.MapSearch;
import com.imooc.web.form.RentSearch;

/**
 * Created by 瓦力.
 */
@Controller
public class HouseController {

    @Autowired
    private IAddressService addressService;

    @Autowired
    private IHouseService houseService;

    @Autowired
    private IUserService userService;

//    @Autowired
//    private ISearchService searchService;

//    /**
//     * 自动补全接口
//     */
//    @GetMapping("rent/house/autocomplete")
//    @ResponseBody
//    public ApiResponse autocomplete(@RequestParam(value = "prefix") String prefix) {
//
//        if (prefix.isEmpty()) {
//            return ApiResponse.ofStatus(ApiResponse.Status.BAD_REQUEST);
//        }
//        ServiceResult<List<String>> result = this.searchService.suggest(prefix);
//        return ApiResponse.ofSuccess(result.getResult());
//    }

    /**
     * 获取支持城市列表
     * @return
     */
    @GetMapping("address/support/cities")
    @ResponseBody
    public ApiResponse getSupportCities() {
        //从业务逻辑获取所有城市
        ServiceMultiResult<SupportAddressDTO> result = addressService.findAllCities();
        //如果获取到的数量为0，则返回没有找到
        if (result.getResultSize() == 0) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        }
        //否则返回城市列表结果
        return ApiResponse.ofSuccess(result.getResult());
    }

    /**
     * 获取对应城市支持区域列表
     * @param cityEnName
     * @return
     */
    @GetMapping("address/support/regions")
    @ResponseBody
    public ApiResponse getSupportRegions(@RequestParam(name = "city_name") String cityEnName) {
        ServiceMultiResult<SupportAddressDTO> addressResult = addressService.findAllRegionsByCityName(cityEnName);
        if (addressResult.getResult() == null || addressResult.getTotal() < 1) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        }
        return ApiResponse.ofSuccess(addressResult.getResult());
    }

    /**
     * 获取具体城市所支持的地铁线路
     * @param cityEnName
     * @return
     */
    @GetMapping("address/support/subway/line")
    @ResponseBody
    public ApiResponse getSupportSubwayLine(@RequestParam(name = "city_name") String cityEnName) {
        List<SubwayDTO> subways = addressService.findAllSubwayByCity(cityEnName);
        if (subways.isEmpty()) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        }

        return ApiResponse.ofSuccess(subways);
    }

    /**
     * 获取对应地铁线路所支持的地铁站点
     * @param subwayId
     * @return
     */
    @GetMapping("address/support/subway/station")
    @ResponseBody
    public ApiResponse getSupportSubwayStation(@RequestParam(name = "subway_id") Long subwayId) {
        List<SubwayStationDTO> stationDTOS = addressService.findAllStationBySubway(subwayId);
        if (stationDTOS.isEmpty()) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        }

        return ApiResponse.ofSuccess(stationDTOS);
    }

    /**
     * 前台 租房页面
     * @param rentSearch
     * @param model
     * @param session
     * @param redirectAttributes
     * @return
     */
    @GetMapping("rent/house")
    public String rentHousePage(@ModelAttribute RentSearch rentSearch,
                                Model model, HttpSession session,
                                RedirectAttributes redirectAttributes) {
        //进入页面必须要有城市，在这判断有没有
        if (rentSearch.getCityEnName() == null) {
            //如果没有就去session中找
            String cityEnNameInSession = (String) session.getAttribute("cityEnName");
            //如果session中还是空的，就跳转，让用户必须选择城市
            if (cityEnNameInSession == null) {
                redirectAttributes.addAttribute("msg", "must_chose_city");
                return "redirect:/index";
            } else {
                //如果session不空，就从session中获取
                rentSearch.setCityEnName(cityEnNameInSession);
            }
        } else {
            //如果城市不为空，就放进session中
            session.setAttribute("cityEnName", rentSearch.getCityEnName());
        }

        //获取所有城市
        ServiceResult<SupportAddressDTO> city = addressService.findCity(rentSearch.getCityEnName());
        if (!city.isSuccess()) {
            redirectAttributes.addAttribute("msg", "must_chose_city");
            return "redirect:/index";
        }
        model.addAttribute("currentCity", city.getResult());

        //获取所有的区域信息
        ServiceMultiResult<SupportAddressDTO> addressResult = addressService.findAllRegionsByCityName(rentSearch.getCityEnName());
        //如果是空，一样的，让用户必须选择城市
        if (addressResult.getResult() == null || addressResult.getTotal() < 1) {
            redirectAttributes.addAttribute("msg", "must_chose_city");
            return "redirect:/index";
        }

        //查询房源
        ServiceMultiResult<HouseDTO> serviceMultiResult = houseService.query(rentSearch);

        //把查到的属性赋值给model
        model.addAttribute("total", serviceMultiResult.getTotal());
        model.addAttribute("houses", serviceMultiResult.getResult());

        //如果拿到的区域名为空，就默认匹配所有的区域
        if (rentSearch.getRegionEnName() == null) {
            rentSearch.setRegionEnName("*");
        }

        model.addAttribute("searchBody", rentSearch);
        model.addAttribute("regions", addressResult.getResult());

        model.addAttribute("priceBlocks", RentValueBlock.PRICE_BLOCK);
        model.addAttribute("areaBlocks", RentValueBlock.AREA_BLOCK);

        model.addAttribute("currentPriceBlock", RentValueBlock.matchPrice(rentSearch.getPriceBlock()));
        model.addAttribute("currentAreaBlock", RentValueBlock.matchArea(rentSearch.getAreaBlock()));

        return "rent-list";
    }

    /**
     * 房源信息详情页面
     * @param houseId
     * @param model
     * @return
     */
    @GetMapping("rent/house/show/{id}")
    public String show(@PathVariable(value = "id") Long houseId,
                       Model model) {
        //如果id不正常，返回404
        if (houseId <= 0) {
            return "404";
        }
        //如果id正常，就查询
        ServiceResult<HouseDTO> serviceResult = houseService.findCompleteOne(houseId);
        //如果没有找到，返回404
        if (!serviceResult.isSuccess()) {
            return "404";
        }
        //找到了，获得查询结果保存到dto
        HouseDTO houseDTO = serviceResult.getResult();
        //在查询城市和区域的具体信息
        Map<SupportAddress.Level, SupportAddressDTO>
                addressMap = addressService.findCityAndRegion(houseDTO.getCityEnName(), houseDTO.getRegionEnName());
        //查出来赋给dto
        SupportAddressDTO city = addressMap.get(SupportAddress.Level.CITY);
        SupportAddressDTO region = addressMap.get(SupportAddress.Level.REGION);
        //把数据渲染进model
        model.addAttribute("city", city);
        model.addAttribute("region", region);

        ServiceResult<UserDTO> userDTOServiceResult = userService.findById(houseDTO.getAdminId());
        model.addAttribute("agent", userDTOServiceResult.getResult());
        model.addAttribute("house", houseDTO);

//        ServiceResult<Long> aggResult = searchService.aggregateDistrictHouse(city.getEnName(), region.getEnName(), houseDTO.getDistrict());
        //聚合数据
        model.addAttribute("houseCountInDistrict", 0);

        return "house-detail";
    }

//    @GetMapping("rent/house/map")
//    public String rentMapPage(@RequestParam(value = "cityEnName") String cityEnName,
//                              Model model,
//                              HttpSession session,
//                              RedirectAttributes redirectAttributes) {
//        ServiceResult<SupportAddressDTO> city = addressService.findCity(cityEnName);
//        if (!city.isSuccess()) {
//            redirectAttributes.addAttribute("msg", "must_chose_city");
//            return "redirect:/index";
//        } else {
//            session.setAttribute("cityName", cityEnName);
//            model.addAttribute("city", city.getResult());
//        }
//
//        ServiceMultiResult<SupportAddressDTO> regions = addressService.findAllRegionsByCityName(cityEnName);
//
//
//        ServiceMultiResult<HouseBucketDTO> serviceResult = searchService.mapAggregate(cityEnName);
//
//        model.addAttribute("aggData", serviceResult.getResult());
//        model.addAttribute("total", serviceResult.getTotal());
//        model.addAttribute("regions", regions.getResult());
//        return "rent-map";
//    }

//    @GetMapping("rent/house/map/houses")
//    @ResponseBody
//    public ApiResponse rentMapHouses(@ModelAttribute MapSearch mapSearch) {
//        if (mapSearch.getCityEnName() == null) {
//            return ApiResponse.ofMessage(HttpStatus.BAD_REQUEST.value(), "必须选择城市");
//        }
//        ServiceMultiResult<HouseDTO> serviceMultiResult;
//        if (mapSearch.getLevel() < 13) {
//            serviceMultiResult = houseService.wholeMapQuery(mapSearch);
//        } else {
//            // 小地图查询必须要传递地图边界参数
//            serviceMultiResult = houseService.boundMapQuery(mapSearch);
//        }
//
//        ApiResponse response = ApiResponse.ofSuccess(serviceMultiResult.getResult());
//        response.setMore(serviceMultiResult.getTotal() > (mapSearch.getStart() + mapSearch.getSize()));
//        return response;
//
//    }
}


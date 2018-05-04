package com.imooc.service.house;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.Valid;

import org.apache.kafka.common.security.auth.Login;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Maps;
import com.imooc.base.HouseSort;
import com.imooc.base.HouseStatus;
//import com.imooc.base.HouseSubscribeStatus;
import com.imooc.base.LoginUserUtil;
import com.imooc.entity.House;
import com.imooc.entity.HouseDetail;
import com.imooc.entity.HousePicture;
import com.imooc.entity.HouseSubscribe;
import com.imooc.entity.HouseTag;
import com.imooc.entity.Subway;
import com.imooc.entity.SubwayStation;
import com.imooc.repository.HouseDetailRepository;
import com.imooc.repository.HousePictureRepository;
import com.imooc.repository.HouseRepository;
import com.imooc.repository.HouseSubscribeRespository;
import com.imooc.repository.HouseTagRepository;
import com.imooc.repository.SubwayRepository;
import com.imooc.repository.SubwayStationRepository;
import com.imooc.service.ServiceMultiResult;
import com.imooc.service.ServiceResult;
//import com.imooc.service.search.ISearchService;
import com.imooc.web.dto.HouseDTO;
import com.imooc.web.dto.HouseDetailDTO;
import com.imooc.web.dto.HousePictureDTO;
import com.imooc.web.dto.HouseSubscribeDTO;
import com.imooc.web.form.DatatableSearch;
import com.imooc.web.form.HouseForm;
import com.imooc.web.form.MapSearch;
import com.imooc.web.form.PhotoForm;
import com.imooc.web.form.RentSearch;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;

@Service
public class HouseServiceImpl implements IHouseService {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private HouseDetailRepository houseDetailRepository;

    @Autowired
    private HousePictureRepository housePictureRepository;

    @Autowired
    private HouseTagRepository houseTagRepository;

    @Autowired
    private SubwayRepository subwayRepository;

    @Autowired
    private SubwayStationRepository subwayStationRepository;

    @Autowired
    private HouseSubscribeRespository subscribeRespository;

    @Autowired
    private IQiNiuService qiNiuService;


    @Value("${qiniu.cdn.prefix}")
    private String cdnPrefix;

    /**
     * 新增房源
     * @param houseForm
     * @return
     */
    @Override
    public ServiceResult<HouseDTO> save(HouseForm houseForm) {

        HouseDetail detail = new HouseDetail();
        ServiceResult<HouseDTO> subwayValidtionResult = wrapperDetailInfo(detail, houseForm);
        if (subwayValidtionResult != null) {
            return subwayValidtionResult;
        }

        //把用户访问的house映射到我们想让其访问的地址map中
        House house = new House();
        modelMapper.map(houseForm,house);

        Date now = new Date();
        house.setCreateTime(now);
        house.setLastUpdateTime(now);
        house.setAdminId(LoginUserUtil.getLoginUserId());
        house = houseRepository.save(house);//保存房源

        detail.setHouseId(house.getId());
        detail = houseDetailRepository.save(detail);//保存房源详细信息

        List<HousePicture> pictures = generatePictures(houseForm, house.getId());
        Iterable<HousePicture> housePictures = housePictureRepository.save(pictures);//保存图片

        //保存完毕就映射出一个dto，用来别人访问
        HouseDTO houseDTO = modelMapper.map(house, HouseDTO.class);
        HouseDetailDTO houseDetailDTO = modelMapper.map(detail, HouseDetailDTO.class);

        houseDTO.setHouseDetail(houseDetailDTO);

        List<HousePictureDTO> pictureDTOS = new ArrayList<>();
        housePictures.forEach(housePicture -> pictureDTOS.add(modelMapper.map(housePicture, HousePictureDTO.class)));
        houseDTO.setPictures(pictureDTOS);//在dto中放置图片
        houseDTO.setCover(this.cdnPrefix + houseDTO.getCover());//在dto中放置封面

        //保存tags
        List<String> tags = houseForm.getTags();
        if (tags != null && !tags.isEmpty()) {
            List<HouseTag> houseTags = new ArrayList<>();
            for (String tag : tags) {
                houseTags.add(new HouseTag(house.getId(), tag));
            }
            houseTagRepository.save(houseTags);
            houseDTO.setTags(tags);
        }

        return new ServiceResult<HouseDTO>(true, null, houseDTO);
    }

    /**
     * 编辑后更新房源
     * @param houseForm
     * @return
     */
    @Override
    @Transactional
    public ServiceResult update(HouseForm houseForm) {
        House house = this.houseRepository.findOne(houseForm.getId());
        if (house == null) {
            return ServiceResult.notFound();
        }

        HouseDetail detail = this.houseDetailRepository.findByHouseId(house.getId());
        if (detail == null) {
            return ServiceResult.notFound();
        }

        ServiceResult wrapperResult = wrapperDetailInfo(detail, houseForm);
        if (wrapperResult != null) {
            return wrapperResult;
        }

        houseDetailRepository.save(detail);

        List<HousePicture> pictures = generatePictures(houseForm, houseForm.getId());
        housePictureRepository.save(pictures);

        if (houseForm.getCover() == null) {
            houseForm.setCover(house.getCover());
        }

        modelMapper.map(houseForm, house);
        house.setLastUpdateTime(new Date());
        houseRepository.save(house);

//        if (house.getStatus() == HouseStatus.PASSES.getValue()) {
//            searchService.index(house.getId());
//        }

        return ServiceResult.success();
    }


    /**
     * 查找所有房源
     * @param searchBody
     * @return
     */
    @Override
    public ServiceMultiResult<HouseDTO> adminQuery(DatatableSearch searchBody) {

        List<HouseDTO> houseDTOS = new ArrayList<>();

        Sort sort = new Sort(Sort.Direction.fromString(searchBody.getDirection()), searchBody.getOrderBy());
        int page = searchBody.getStart() / searchBody.getLength();

        Pageable pageable = new PageRequest(page, searchBody.getLength(), sort);

        Specification<House> specification = (root, query, cb) -> {
            Predicate predicate = cb.equal(root.get("adminId"), LoginUserUtil.getLoginUserId());
            predicate = cb.and(predicate, cb.notEqual(root.get("status"), HouseStatus.DELETED.getValue()));

            if (searchBody.getCity() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("cityEnName"), searchBody.getCity()));
            }

            if (searchBody.getStatus() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), searchBody.getStatus()));
            }

            if (searchBody.getCreateTimeMin() != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createTime"), searchBody.getCreateTimeMin()));
            }

            if (searchBody.getCreateTimeMax() != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("createTime"), searchBody.getCreateTimeMax()));
            }

            if (searchBody.getTitle() != null) {
                predicate = cb.and(predicate, cb.like(root.get("title"), "%" + searchBody.getTitle() + "%"));
            }

            return predicate;
        };

        Page<House> houses = houseRepository.findAll(specification, pageable);
        houses.forEach(house -> {
            HouseDTO houseDTO = modelMapper.map(house, HouseDTO.class);
            houseDTO.setCover(this.cdnPrefix + house.getCover());
            houseDTOS.add(houseDTO);
        });

        return new ServiceMultiResult<>(houses.getTotalElements(), houseDTOS);
    }

    /**
     * 查询单个房源完整信息
     * @param id
     * @return
     */
    @Override
    public ServiceResult<HouseDTO> findCompleteOne(Long id) {
        House house = houseRepository.findOne(id);
        if (house == null) {
            return ServiceResult.notFound();
        }

        HouseDetail detail = houseDetailRepository.findByHouseId(id);
        List<HousePicture> pictures = housePictureRepository.findAllByHouseId(id);

        HouseDetailDTO detailDTO = modelMapper.map(detail, HouseDetailDTO.class);
        List<HousePictureDTO> pictureDTOS = new ArrayList<>();
        for (HousePicture picture : pictures) {
            HousePictureDTO pictureDTO = modelMapper.map(picture, HousePictureDTO.class);
            pictureDTOS.add(pictureDTO);
        }


        List<HouseTag> tags = houseTagRepository.findAllByHouseId(id);
        List<String> tagList = new ArrayList<>();
        for (HouseTag tag : tags) {
            tagList.add(tag.getName());
        }

        HouseDTO result = modelMapper.map(house, HouseDTO.class);
        result.setHouseDetail(detailDTO);
        result.setPictures(pictureDTOS);
        result.setTags(tagList);

        if (LoginUserUtil.getLoginUserId() > 0) { // 已登录用户
            HouseSubscribe subscribe = subscribeRespository.findByHouseIdAndUserId(house.getId(), LoginUserUtil.getLoginUserId());
            if (subscribe != null) {
                result.setSubscribeStatus(subscribe.getStatus());
            }
        }

        return ServiceResult.of(result);
    }

    /**
     * 移除图片
     * @param id
     * @return
     */
    @Override
    public ServiceResult removePhoto(Long id) {
        HousePicture picture = housePictureRepository.findOne(id);
        if (picture == null) {
            return ServiceResult.notFound();
        }

        try {
            Response response = this.qiNiuService.delete(picture.getPath());
            if (response.isOK()) {
                housePictureRepository.delete(id);
                return ServiceResult.success();
            } else {
                return new ServiceResult(false, response.error);
            }
        } catch (QiniuException e) {
            e.printStackTrace();
            return new ServiceResult(false, e.getMessage());
        }
    }

    /**
     * 更新封面
     * @param coverId
     * @param targetId
     * @return
     */
    @Override
    @Transactional
    public ServiceResult updateCover(Long coverId, Long targetId) {
        HousePicture cover = housePictureRepository.findOne(coverId);
        if (cover == null) {
            return ServiceResult.notFound();
        }

        houseRepository.updateCover(targetId, cover.getPath());
        return ServiceResult.success();
    }

    /**
     * 增加标签
     * @param houseId
     * @param tag
     * @return
     */
    @Override
    @Transactional
    public ServiceResult addTag(Long houseId, String tag) {
        House house = houseRepository.findOne(houseId);
        if (house == null) {
            return ServiceResult.notFound();
        }

        HouseTag houseTag = houseTagRepository.findByNameAndHouseId(tag, houseId);
        if (houseTag != null) {
            return new ServiceResult(false, "标签已存在");
        }

        houseTagRepository.save(new HouseTag(houseId, tag));
        return ServiceResult.success();
    }

    /**
     * 删除标签
     * @param houseId
     * @param tag
     * @return
     */
    @Override
    @Transactional
    public ServiceResult removeTag(Long houseId, String tag) {
        House house = houseRepository.findOne(houseId);
        if (house == null) {
            return ServiceResult.notFound();
        }

        HouseTag houseTag = houseTagRepository.findByNameAndHouseId(tag, houseId);
        if (houseTag == null) {
            return new ServiceResult(false, "标签不存在");
        }

        houseTagRepository.delete(houseTag.getId());
        return ServiceResult.success();
    }


    /**
     * 更新房源状态
     * @param id
     * @param status
     * @return
     */
    @Override
    @Transactional
    public ServiceResult updateStatus(Long id, int status) {
        House house = houseRepository.findOne(id);
        if (house == null) {
            return ServiceResult.notFound();
        }

        if (house.getStatus() == status) {
            return new ServiceResult(false, "状态没有发生变化");
        }

        if (house.getStatus() == HouseStatus.RENTED.getValue()) {
            return new ServiceResult(false, "已出租的房源不允许修改状态");
        }

        if (house.getStatus() == HouseStatus.DELETED.getValue()) {
            return new ServiceResult(false, "已删除的资源不允许操作");
        }

        houseRepository.updateStatus(id, status);

        // 上架更新索引 其他情况都要删除索引
//        if (status == HouseStatus.PASSES.getValue()) {
//            searchService.index(id);
//        } else {
//            searchService.remove(id);
//        }
        return ServiceResult.success();
    }



    /**
     * 图片对象列表信息填充
     * @param form
     * @param houseId
     * @return
     */
    private List<HousePicture> generatePictures(HouseForm form, Long houseId) {
        List<HousePicture> pictures = new ArrayList<>();
        if (form.getPhotos() == null || form.getPhotos().isEmpty()) {
            return pictures;
        }

        for (PhotoForm photoForm : form.getPhotos()) {
            HousePicture picture = new HousePicture();
            picture.setHouseId(houseId);
            picture.setCdnPrefix(cdnPrefix);
            picture.setPath(photoForm.getPath());
            picture.setWidth(photoForm.getWidth());
            picture.setHeight(photoForm.getHeight());
            pictures.add(picture);
        }
        return pictures;
    }


    /**
     * 房源详细信息对象填充
     * @param houseDetail
     * @param houseForm
     * @return
     */
    private ServiceResult<HouseDTO> wrapperDetailInfo(HouseDetail houseDetail, HouseForm houseForm) {
        Subway subway = subwayRepository.findOne(houseForm.getSubwayLineId());
        if (subway == null) {
            return new ServiceResult<>(false, "Not valid subway line!");
        }

        SubwayStation subwayStation = subwayStationRepository.findOne(houseForm.getSubwayStationId());
        if (subwayStation == null || subway.getId() != subwayStation.getSubwayId()) {
            return new ServiceResult<>(false, "Not valid subway station!");
        }

        houseDetail.setSubwayLineId(subway.getId());
        houseDetail.setSubwayLineName(subway.getName());

        houseDetail.setSubwayStationId(subwayStation.getId());
        houseDetail.setSubwayStationName(subwayStation.getName());

        houseDetail.setDescription(houseForm.getDescription());
        houseDetail.setDetailAddress(houseForm.getDetailAddress());
        houseDetail.setLayoutDesc(houseForm.getLayoutDesc());
        houseDetail.setRentWay(houseForm.getRentWay());
        houseDetail.setRoundService(houseForm.getRoundService());
        houseDetail.setTraffic(houseForm.getTraffic());
        return null;

    }


    /**
     * 查询房源
     * @param rentSearch
     * @return
     */
//    @Override
//    public ServiceMultiResult<HouseDTO> query(RentSearch rentSearch) {
//        if (rentSearch.getKeywords() != null && !rentSearch.getKeywords().isEmpty()) {
//            ServiceMultiResult<Long> serviceResult = searchService.query(rentSearch);
//            if (serviceResult.getTotal() == 0) {
//                return new ServiceMultiResult<>(0, new ArrayList<>());
//            }
//
//            return new ServiceMultiResult<>(serviceResult.getTotal(), wrapperHouseResult(serviceResult.getResult()));
//        }
//
//        return simpleQuery(rentSearch);
//
//    }


    /**
     * 粗略查询
     * @param rentSearch
     * @return
     */
    public ServiceMultiResult<HouseDTO> query(RentSearch rentSearch) {
        // 用最近更新时间来默认排序房源
        Sort sort = HouseSort.generateSort(rentSearch.getOrderBy(), rentSearch.getOrderDirection());
        // 获取分页页码
        int page = rentSearch.getStart() / rentSearch.getSize();
        //分页器初始化
        Pageable pageable = new PageRequest(page, rentSearch.getSize(), sort);

        //设置什么样的房源能被查出来
        Specification<House> specification = (root, criteriaQuery, criteriaBuilder) -> {
            //只有通过审核的房源才行
            Predicate predicate = criteriaBuilder.equal(root.get("status"), HouseStatus.PASSES.getValue());
            //必须符合我们选定的城市
            predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("cityEnName"), rentSearch.getCityEnName()));
            //我们设置的默认地铁距离是-1，要参加排序，必须不能是-1，不然每次都排在前面
            if (HouseSort.DISTANCE_TO_SUBWAY_KEY.equals(rentSearch.getOrderBy())) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.gt(root.get(HouseSort.DISTANCE_TO_SUBWAY_KEY), -1));
            }
            return predicate;
        };
        //查询
        Page<House> houses = houseRepository.findAll(specification, pageable);
        //转成dto对象
        List<HouseDTO> houseDTOS = new ArrayList<>();

        List<Long> houseIds = new ArrayList<>();
        Map<Long, HouseDTO> idToHouseMap = Maps.newHashMap();
        houses.forEach(house -> {
            HouseDTO houseDTO = modelMapper.map(house, HouseDTO.class);
            houseDTO.setCover(this.cdnPrefix + house.getCover());
            houseDTOS.add(houseDTO);

            houseIds.add(house.getId());
            idToHouseMap.put(house.getId(), houseDTO);
        });


        wrapperHouseList(houseIds, idToHouseMap);
        return new ServiceMultiResult<>(houses.getTotalElements(), houseDTOS);
    }

    /**
     * 渲染详细信息 及 标签
     * @param houseIds
     * @param idToHouseMap
     */
    private void wrapperHouseList(List<Long> houseIds, Map<Long, HouseDTO> idToHouseMap) {
        List<HouseDetail> details = houseDetailRepository.findAllByHouseIdIn(houseIds);
        details.forEach(houseDetail -> {
            HouseDTO houseDTO = idToHouseMap.get(houseDetail.getHouseId());
            HouseDetailDTO detailDTO = modelMapper.map(houseDetail, HouseDetailDTO.class);
            houseDTO.setHouseDetail(detailDTO);
        });

        List<HouseTag> houseTags = houseTagRepository.findAllByHouseIdIn(houseIds);
        houseTags.forEach(houseTag -> {
            HouseDTO house = idToHouseMap.get(houseTag.getHouseId());
            house.getTags().add(houseTag.getName());
        });
    }


}

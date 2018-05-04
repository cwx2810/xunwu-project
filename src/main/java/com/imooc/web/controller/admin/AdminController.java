package com.imooc.web.controller.admin;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.google.common.base.Strings;
import com.google.gson.Gson;
//import com.imooc.base.ApiDataTableResponse;
import com.imooc.base.ApiResponse;
//import com.imooc.base.HouseOperation;
//import com.imooc.base.HouseStatus;
//import com.imooc.entity.HouseDetail;
//import com.imooc.entity.SupportAddress;
//import com.imooc.service.IUserService;
//import com.imooc.service.ServiceMultiResult;
//import com.imooc.service.ServiceResult;
//import com.imooc.service.house.IAddressService;
//import com.imooc.service.house.IHouseService;
//import com.imooc.service.house.IQiNiuService;
//import com.imooc.web.dto.HouseDTO;
//import com.imooc.web.dto.HouseDetailDTO;
//import com.imooc.web.dto.HouseSubscribeDTO;
//import com.imooc.web.dto.QiNiuPutRet;
//import com.imooc.web.dto.SubwayDTO;
//import com.imooc.web.dto.SubwayStationDTO;
//import com.imooc.web.dto.SupportAddressDTO;
//import com.imooc.web.dto.UserDTO;
//import com.imooc.web.form.DatatableSearch;
//import com.imooc.web.form.HouseForm;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;


@Controller
public class AdminController {

    /**
     * 后台管理中心
     * @return
     */
    @GetMapping("/admin/center")
    public String adminCenterPage() {
        return "admin/center";
    }

    /**
     * 欢迎页
     * @return
     */
    @GetMapping("/admin/welcome")
    public String welcomePage() {
        return "admin/welcome";
    }

    /**
     * 管理员登录页
     * @return
     */
    @GetMapping("/admin/login")
    public String adminLoginPage() {
        return "admin/login";
    }

    /**
     * 房源列表页
     * @return
     */
    @GetMapping("admin/house/list")
    public String houseListPage() {
        return "admin/house-list";
    }

    /**
     * 新增房源功能页
     * @return
     */
    @GetMapping("admin/add/house")
    public String addHousePage() {
        return "admin/house-add";
    }

    /**
     * 上传图片接口
     * @param file
     * @return
     */
    @PostMapping(value = "admin/upload/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ApiResponse uploadPhoto(@RequestParam("file") MultipartFile file) {
        //如果上传为空，则提示参数不正确
        if (file.isEmpty()) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_VALID_PARAM);
        }
        //如果上传不为空，则记录文件名字
        String fileName = file.getOriginalFilename();
        File target = new File("C:/project/xunwu-project/tmp/" + fileName);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            e.printStackTrace();
            return ApiResponse.ofStatus(ApiResponse.Status.INTERNAL_SERVER_ERROR);
        }
        return ApiResponse.ofSuccess(null);

        //七牛云相关
//        try {
//            InputStream inputStream = file.getInputStream();
//            Response response = qiNiuService.uploadFile(inputStream);
//            if (response.isOK()) {
//                QiNiuPutRet ret = gson.fromJson(response.bodyString(), QiNiuPutRet.class);
//                return ApiResponse.ofSuccess(ret);
//            } else {
//                return ApiResponse.ofMessage(response.statusCode, response.getInfo());
//            }
//
//        } catch (QiniuException e) {
//            Response response = e.response;
//            try {
//                return ApiResponse.ofMessage(response.statusCode, response.bodyString());
//            } catch (QiniuException e1) {
//                e1.printStackTrace();
//                return ApiResponse.ofStatus(ApiResponse.Status.INTERNAL_SERVER_ERROR);
//            }
//        } catch (IOException e) {
//            return ApiResponse.ofStatus(ApiResponse.Status.INTERNAL_SERVER_ERROR);
//        }
    }


}

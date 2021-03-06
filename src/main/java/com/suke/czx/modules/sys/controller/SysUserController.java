package com.suke.czx.modules.sys.controller;

import cn.hutool.core.map.MapUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.suke.czx.common.annotation.AuthIgnore;
import com.suke.czx.common.annotation.SysLog;
import com.suke.czx.common.base.AbstractController;
import com.suke.czx.common.utils.Constant;
import com.suke.czx.common.utils.R;
import com.suke.czx.common.validator.Assert;
import com.suke.czx.common.validator.ValidatorUtils;
import com.suke.czx.modules.sys.entity.SysUser;
import com.suke.czx.modules.sys.entity.SysUserRole;
import com.suke.czx.modules.sys.service.SysUserRoleService;
import com.suke.czx.modules.sys.service.SysUserService;
import lombok.AllArgsConstructor;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统用户
 * 
 * @author czx
 * @email object_czx@163.com
 * @date 2016年10月31日 上午10:40:10
 */

@RestController
@RequestMapping("/sys/user")
@AllArgsConstructor
public class SysUserController extends AbstractController {

	private final SysUserService sysUserService;
	private final SysUserRoleService sysUserRoleService;
	private final PasswordEncoder passwordEncoder;

	/**
	 * 所有用户列表
	 */
	@RequestMapping("/list")
	@PreAuthorize("hasRole('sys:user:list')")
	public R list(@RequestParam Map<String, Object> params){
		//只有超级管理员，才能查看所有管理员列表
		if(getUserId() != Constant.SUPER_ADMIN){
			params.put("createUserId", getUserId());
		}

		//查询列表数据
		QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
		if(MapUtil.getStr(params,"key") != null){
			queryWrapper
					.like("username",MapUtil.getStr(params,"key"))
					.or()
					.like("mobile",MapUtil.getStr(params,"key"));
		}
		IPage<SysUser> sysConfigList = sysUserService.page(mpPageConvert.<SysUser>pageParamConvert(params),queryWrapper);

		return R.ok().put("page", mpPageConvert.pageValueConvert(sysConfigList));
	}
	
	/**
	 * 获取登录的用户信息
	 */
	@RequestMapping("/info")
	public R info(){
		return R.ok().put("user", getUser());
	}
	
	/**
	 * 修改登录用户密码
	 */
	@SysLog("修改密码")
	@RequestMapping("/password")
	public R password(String password, String newPassword){
		Assert.isBlank(newPassword, "新密码不为能空");
		password = passwordEncoder.encode(password);
		newPassword = passwordEncoder.encode(newPassword);

		SysUser user = sysUserService.getById(getUserId());
		if(!passwordEncoder.matches(password,user.getPassword())){
			return R.error("原密码不正确");
		}
		//更新密码
		sysUserService.updatePassword(getUserId(), password, newPassword);
		return R.ok();
	}
	
	/**
	 * 用户信息
	 */
	@RequestMapping("/info/{userId}")
	@PreAuthorize("hasRole('sys:user:info')")
	public R info(@PathVariable("userId") Long userId){
		SysUser user = sysUserService.getById(userId);

		//获取用户所属的角色列表
		List<Long> roleIdList = sysUserRoleService.list(
				        new QueryWrapper<SysUserRole>()
                        .lambda()
                        .eq(SysUserRole::getUserId,userId)
		        ).stream()
                .map(sysUserRole ->sysUserRole.getRoleId())
                .collect(Collectors.toList());

		user.setRoleIdList(roleIdList);
		return R.ok().put("user", user);
	}

	/**
	 * 根据用户名查询用户是否存在
	 */
	@AuthIgnore
	@RequestMapping("/getUserByName")
	public R judgeUernameCanUse(@RequestBody SysUser user){
		String username = user.getUsername();
		SysUser queryUser =	sysUserService.getOne(new QueryWrapper<SysUser>().eq("username", username));
		if (queryUser != null) {
			return R.error(1, "用户名已存在");
		} else {
			return R.ok();
		}
	}
	
	/**
	 * 管理员操作保存用户
	 */
	@SysLog("保存用户")
	@RequestMapping("/save")
	@PreAuthorize("hasRole('sys:user:save')")
	public R save(@RequestBody SysUser user){
		ValidatorUtils.validateEntity(user);
		
		user.setCreateUserId(getUserId());
		sysUserService.saveUserRole(user);
		
		return R.ok();
	}

	/**
	 * 用户注册
	 */
	@AuthIgnore
	@SysLog("用户注册")
	@RequestMapping("/register")
	public R register(HttpServletRequest request){
		SysUser user = new SysUser();
		String username = request.getParameter("username");
		String password = request.getParameter("password");
		SysUser user1 =	sysUserService.getOne(new QueryWrapper<SysUser>().eq("username", username));
		if (user1 != null) {
			return R.error(1, "用户名已存在");
		}
		List<Long> roleIdList = new ArrayList<>();
		long roleId = 9L;
		roleIdList.add(roleId);
		user.setUsername(username);
		user.setPassword(password);
		user.setRoleIdList(roleIdList);
		user.setStatus(1);
		user.setEmail("321@163.com");
		ValidatorUtils.validateEntity(user);
		user.setCreateUserId(getUserId());
		sysUserService.saveUserRole(user);

		return R.ok();
	}
	
	/**
	 * 修改用户
	 */
	@SysLog("修改用户")
	@RequestMapping("/update")
	@PreAuthorize("hasRole('sys:user:update')")
	public R update(@RequestBody SysUser user){
		ValidatorUtils.validateEntity(user);
		
		user.setCreateUserId(getUserId());
		sysUserService.updateUserRole(user);
		
		return R.ok();
	}
	
	/**
	 * 删除用户
	 */
	@SysLog("删除用户")
	@RequestMapping("/delete")
	@PreAuthorize("hasRole('sys:user:delete')")
	public R delete(@RequestBody Long[] userIds){
		if(ArrayUtils.contains(userIds, 1L)){
			return R.error("系统管理员不能删除");
		}
		
		if(ArrayUtils.contains(userIds, getUserId())){
			return R.error("当前用户不能删除");
		}
		sysUserService.removeByIds(Arrays.asList(userIds));
		return R.ok();
	}
}

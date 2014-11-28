function set_as_proxy(mac){
	//console.log('set as proxy');
	$.ajax({
	            url:hackBase + "/wm/proxycache/addproxy/" + mac + "/json",
	            dataType:"text",
	            success:function (data) {
	            	//console.log('add proxy success!!!!');
	                //alert("success!!!");
	                alert(data);
	            }
	        });

}
function set_as_proxy(mac){
	//console.log('set as proxy');
	$.ajax({
	            url:hackBase + "/wm/proxycache/setglobalproxy/" + mac + "/json",
	            dataType:"text",
	            success:function (data) {
	            	//console.log('add proxy success!!!!');
	                //alert("success!!!");
	                alert(data);
	            }
	        });

}

function new_proxyrule(){
	$('#content').html(new NewProxyruleView({model:hl}).render().el);
    $('ul.nav > li').removeClass('active');
    $('li > a[href*="/hosts"]').parent().addClass('active');
}
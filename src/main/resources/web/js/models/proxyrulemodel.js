
window.Proxyrule = Backbone.Model.extend({

    defaults: {
        ruleid: -1,
        client_ip: 'unknown',
        client_port: 'unknown',
        server_ip: 'unknown',
        server_port: 'unknown',
    },

    // initialize:function () {}

});

window.ProxyruleCollection = Backbone.Collection.extend({

    model:Proxyrule,

    fetch:function () {
        var self = this;
        console.log("fetching proxyrule list")
        $.ajax({
            url:hackBase + "/wm/proxycache/getproxyrulelist/json",
            dataType:"json",
            success:function (data) {
                console.log("fetched  proxyrule list: " + data.length);
                console.log(data);
                var old_ids = self.pluck('id');
                _.each(data, function(pr) {
                	pr.id = 'client<' + pr.client_ip + ':' + pr.client_port + '>' 
                			+ 'proxy<' + pr.proxy.device.mac + '>';
                	old_ids = _.without(old_ids, pr.id);
                    self.add(pr, {silent: true});
                });
                _.each(old_ids, function(pr) {
                    console.log("---removing proxyrule " + pr);
                    self.remove({id:pr});
                });
                 self.trigger('add'); // batch redraws
            }
        });

    },

});

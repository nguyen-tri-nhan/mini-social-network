import _ from 'lodash';
import React from 'react';
import PageComponent from "../component/PageComponent";

class Login extends PageComponent{

    user = null;
    constructor(props){
        super(props);
        this.user = {
            username: '',
            password: '',
        }
        this.login = this.login.bind(this);
    }

    login(e){

    }

    render(){
        return(
            <div></div>
        );
    }

}

export default Login
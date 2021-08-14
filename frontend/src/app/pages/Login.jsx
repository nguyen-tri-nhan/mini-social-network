import { useState } from 'react';
import {
    Card,
    CardHeader,
    CardBody,
    Col,
    Row,
    CardFooter,
    Form,
    FormGroup,
    Input,
    Label,
    Button
} from 'reactstrap';
import MAuth from '../model/MAuth'
import { useHistory } from 'react-router-dom';

export default function Login() {

    const history = useHistory();

    const [usernameOrEmail, setUsernameOrEmail] = useState('');
    const [password, setPassword] = useState('');

    const onSubmit = (e) => {
        e.preventDefault();
        MAuth.login({usernameOrEmail, password})
            .then(() => MAuth.isLoggedIn() && history.push("/"));
    }

    const onUserNameChange = (e) => {
        setUsernameOrEmail(e.target.value);
    }

    const onPasswordChange = (e) => {
        setPassword(e.target.value);
    }
    return (
        <div>
            <Row xl="12" sm="12" xs="12" md="12">
                <Col xl="12" sm="12" xs="12" md="12">
                    <Card className="login-card">
                        <CardHeader>
                            <h3>Đăng nhập</h3>
                        </CardHeader>
                        <CardBody>
                            <Form onSubmit={onSubmit}>
                                <FormGroup>
                                    <Input placeholder="Tên đăng nhập hoặc email" 
                                            name="usernameOrEmail" 
                                            onChange={onUserNameChange} 
                                            required/>
                                </FormGroup>
                                <FormGroup>
                                    <Input placeholder="Mật khẩu"
                                            name="password" 
                                            type="password"
                                            onChange={onPasswordChange} 
                                            required/>
                                </FormGroup>
                                <button className="btn btn-primary" type="submit">Đăng nhập</button>
                            </Form>
                        </CardBody>
                        <CardFooter>
                            <p>Chưa có tài khoản? <a href="/signup">Đăng ký ngay!</a></p>
                        </CardFooter>
                    </Card>
                </Col>
            </Row>
        </div>
    );
}
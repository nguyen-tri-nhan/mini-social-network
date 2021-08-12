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
            <Row>
                <Col>
                    <Card>
                        <CardHeader>
                            <h3>Đăng nhập</h3>
                        </CardHeader>
                        <CardBody>
                            <Form onSubmit={onSubmit}>
                                <FormGroup>
                                    <Label for="usernameOrEmail">Tên đăng nhập hoặc email</Label>
                                    <Input name="usernameOrEmail" onChange={onUserNameChange}/>
                                </FormGroup>
                                <FormGroup>
                                    <Label for="password">Mật khẩu</Label>
                                    <Input name="password" type="password"onChange={onPasswordChange}/>
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